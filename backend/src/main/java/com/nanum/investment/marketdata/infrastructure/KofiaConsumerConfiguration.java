package com.nanum.investment.marketdata.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

/** Portable V1/V2 adapter. A consumer never reads or writes its stale local KOFIA tables. */
@Configuration
@ConditionalOnProperty(name = "kofia.consumer.enabled", havingValue = "true")
public class KofiaConsumerConfiguration {
  @Bean
  static BeanFactoryPostProcessor removeLocalCollectionTriggers() {
    return factory -> {
      BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
      for (String name : registry.getBeanDefinitionNames()) {
        String type = registry.getBeanDefinition(name).getBeanClassName();
        if (type != null
            && type.startsWith("com.nanum.investment.marketdata.")
            && type.matches(".*\\.Kofia.*(Scheduler|Recovery)")) {
          registry.removeBeanDefinition(name);
        }
      }
    };
  }

  @Bean
  static BeanPostProcessor preventLocalKofiaAccess() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
        String type = ClassUtils.getUserClass(bean).getName();
        boolean reprocessing =
            type.equals(
                "com.nanum.investment.marketdata.application.CollectionJobReprocessingService");
        if (!reprocessing
            && (!type.startsWith("com.nanum.investment.marketdata.")
                || !type.matches(
                    ".*\\.Kofia.*(Service|Repository|JobRunner|RestClient|CatalogClient)")))
          return bean;
        ProxyFactory proxy = new ProxyFactory(bean);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(
            (MethodInterceptor)
                invocation -> {
                  if (invocation.getMethod().getDeclaringClass() == Object.class)
                    return invocation.proceed();
                  if (reprocessing
                      && (invocation.getArguments().length == 0
                          || !"KOFIA"
                              .equalsIgnoreCase(String.valueOf(invocation.getArguments()[0]))))
                    return invocation.proceed();
                  throw new IllegalStateException(
                      "KOFIA consumer: use the shared collector API; local access is disabled");
                });
        return proxy.getProxy();
      }
    };
  }

  @Bean
  RestClient kofiaSharedReadClient(@Value("${kofia.consumer.base-url}") String baseUrl) {
    URI uri = URI.create(baseUrl);
    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null
        || uri.getUserInfo() != null
        || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
      throw new IllegalArgumentException("KOFIA collector base URL must be an HTTP(S) origin");
    }
    var requestFactory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    requestFactory.setReadTimeout(Duration.ofSeconds(30));
    return RestClient.builder()
        .baseUrl(baseUrl.replaceAll("/$", ""))
        .requestFactory(requestFactory)
        .build();
  }

  @Bean
  FilterRegistrationBean<ConsumerFilter> kofiaSharedReadFilter(RestClient kofiaSharedReadClient) {
    var registration = new FilterRegistrationBean<>(new ConsumerFilter(kofiaSharedReadClient));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    registration.addUrlPatterns("/api/v1/kofia/*", "/api/v1/collection-reprocessing/*");
    return registration;
  }

  public static class ConsumerFilter extends OncePerRequestFilter {
    private final RestClient client;

    public ConsumerFilter(RestClient client) {
      this.client = client;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
      String path = request.getRequestURI().substring(request.getContextPath().length());
      boolean kofia = path.equals("/api/v1/kofia") || path.startsWith("/api/v1/kofia/");
      boolean reprocessing =
          path.toLowerCase(Locale.ROOT).startsWith("/api/v1/collection-reprocessing/kofia/");
      if (!kofia && !reprocessing) {
        chain.doFilter(request, response);
        return;
      }
      if (!"GET".equals(request.getMethod()) || reprocessing) {
        error(
            response,
            409,
            "KOFIA_COLLECTION_MOVED",
            "Use the shared collector to manage collection jobs");
        return;
      }
      // String templates must not interpret provider query values containing braces or percent
      // escapes.
      String target =
          path + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
      try {
        client
            .get()
            .uri(builder -> URI.create(builder.build().toString() + target))
            .exchange(
                (outbound, upstream) -> {
                  response.setStatus(upstream.getStatusCode().value());
                  response.setContentType("application/json");
                  response.setHeader("X-Kofia-Source", "shared-collector");
                  response.setHeader("Cache-Control", "no-store");
                  upstream.getBody().transferTo(response.getOutputStream());
                  return null;
                });
      } catch (RuntimeException error) {
        if (!response.isCommitted()) {
          response.reset();
          error(
              response,
              503,
              "KOFIA_COLLECTOR_UNAVAILABLE",
              "Shared collector is unavailable; local fallback is disabled");
        }
      }
    }

    private static void error(HttpServletResponse response, int status, String code, String message)
        throws IOException {
      response.setStatus(status);
      response.setContentType("application/json");
      response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
  }
}
