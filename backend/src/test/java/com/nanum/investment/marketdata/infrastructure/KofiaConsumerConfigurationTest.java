package com.nanum.investment.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class KofiaConsumerConfigurationTest {
  private HttpServer server;
  private KofiaConsumerConfiguration.ConsumerFilter filter;
  private final AtomicReference<String> observed = new AtomicReference<>();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          observed.set(exchange.getRequestURI().toASCIIString());
          byte[] body =
              "[{\"BASE_DT\":\"2026-10-02\",\"value\":123.45}]".getBytes(StandardCharsets.UTF_8);
          int status = exchange.getRequestURI().getPath().endsWith("missing") ? 404 : 200;
          exchange.sendResponseHeaders(status, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    filter =
        new KofiaConsumerConfiguration.ConsumerFilter(
            new KofiaConsumerConfiguration()
                .kofiaSharedReadClient("http://127.0.0.1:" + server.getAddress().getPort()));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void readsSharedSourcePreservingQueryAndDecimalValuesWithoutLocalController() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/kofia/credit-balances");
    request.setQueryString("from=2026-10-01&to=2026-10-02&name=a%20b");
    var response = new MockHttpServletResponse();
    filter.doFilter(
        request,
        response,
        (r, s) -> {
          throw new AssertionError("local controller used");
        });
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).contains("123.45");
    assertThat(response.getHeader("X-Kofia-Source")).isEqualTo("shared-collector");
    assertThat(observed.get())
        .isEqualTo("/api/v1/kofia/credit-balances?from=2026-10-01&to=2026-10-02&name=a%20b");
  }

  @Test
  void forwardsUpstreamNotFound() throws Exception {
    var response = new MockHttpServletResponse();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/v1/kofia/missing"),
        response,
        (r, s) -> {
          throw new AssertionError("local fallback");
        });
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void unavailableSourceNeverFallsBackToLocalData() throws Exception {
    server.stop(0);
    var response = new MockHttpServletResponse();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/v1/kofia/datasets"),
        response,
        (r, s) -> {
          throw new AssertionError("local fallback");
        });
    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentAsString()).contains("KOFIA_COLLECTOR_UNAVAILABLE");
  }

  @Test
  void forbidsCollectionAndGenericReprocessingWithoutCallingUpstream() throws Exception {
    for (String path :
        new String[] {
          "/api/v1/kofia/collection-jobs", "/api/v1/collection-reprocessing/kOfIa/123"
        }) {
      var response = new MockHttpServletResponse();
      filter.doFilter(
          new MockHttpServletRequest("POST", path),
          response,
          (r, s) -> {
            throw new AssertionError("local mutation");
          });
      assertThat(response.getStatus()).isEqualTo(409);
    }
    assertThat(observed.get()).isNull();
  }

  @Test
  void otherProvidersRemainLocal() throws Exception {
    var chain = mock(jakarta.servlet.FilterChain.class);
    var request = new MockHttpServletRequest("POST", "/api/v1/collection-reprocessing/KRX/123");
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
  }

  @Test
  void removesScheduledAndStartupRecoveryBeforeInstantiation() {
    var factory = new DefaultListableBeanFactory();
    var scheduler = new RootBeanDefinition();
    scheduler.setBeanClassName(
        "com.nanum.investment.marketdata.application.scheduler.KofiaCollectionScheduler");
    factory.registerBeanDefinition("scheduler", scheduler);
    var recovery = new RootBeanDefinition();
    recovery.setBeanClassName("com.nanum.investment.marketdata.application.KofiaFundFlowRecovery");
    factory.registerBeanDefinition("recovery", recovery);
    factory.registerBeanDefinition("unrelated", new RootBeanDefinition(String.class));
    KofiaConsumerConfiguration.removeLocalCollectionTriggers().postProcessBeanFactory(factory);
    assertThat(factory.getBeanDefinitionNames()).containsExactly("unrelated");
  }

  @Test
  void internalCallsCannotReadOrMutateLocalKofiaTables() {
    var jdbc = mock(org.springframework.jdbc.core.simple.JdbcClient.class);
    var target = new KofiaRepository(jdbc, new com.fasterxml.jackson.databind.ObjectMapper());
    var guarded =
        (KofiaRepository)
            KofiaConsumerConfiguration.preventLocalKofiaAccess()
                .postProcessAfterInitialization(target, "kofiaRepository");
    assertThatThrownBy(() -> guarded.creditBalances(LocalDate.now(), LocalDate.now(), 1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("shared collector");
    verifyNoInteractions(jdbc);
  }
}
