package com.nanum.investment.common.infrastructure.external;

import static org.assertj.core.api.Assertions.*;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.*;

class ExternalApiRetryExecutorTest {
  @Test
  void retriesThreeTimesAfterInitialAttempt() {
    AtomicInteger calls = new AtomicInteger();
    TestExecutor executor = new TestExecutor();
    assertThatThrownBy(
            () ->
                executor.execute(
                    () -> {
                      calls.incrementAndGet();
                      throw response(503, null);
                    }))
        .isInstanceOf(HttpServerErrorException.class);
    assertThat(calls).hasValue(4);
    assertThat(executor.delays).hasSize(3);
    assertThat(executor.delays.get(0).toMillis()).isBetween(800L, 1200L);
    assertThat(executor.delays.get(1).toMillis()).isBetween(2400L, 3600L);
    assertThat(executor.delays.get(2).toMillis()).isBetween(8000L, 12000L);
  }

  @Test
  void retriesOnlyConfiguredHttpStatuses() {
    for (int status : List.of(408, 429, 500, 502, 503, 504))
      assertThat(ExternalHttpRetry.isRetryable(response(status, null))).isTrue();
    for (int status : List.of(400, 401, 403, 404, 422))
      assertThat(ExternalHttpRetry.isRetryable(response(status, null))).isFalse();
  }

  @Test
  void retriesTimeoutButNotValidationOrSchemaFailure() {
    assertThat(
            ExternalHttpRetry.isRetryable(
                new ResourceAccessException("timeout", new SocketTimeoutException())))
        .isTrue();
    assertThat(ExternalHttpRetry.isRetryable(new IllegalArgumentException("invalid parameter")))
        .isFalse();
    assertThat(ExternalHttpRetry.isRetryable(new RestClientException("invalid schema"))).isFalse();
  }

  @Test
  void retryAfterTakesPriorityFor429() {
    AtomicInteger calls = new AtomicInteger();
    TestExecutor executor = new TestExecutor();
    assertThatThrownBy(
        () ->
            executor.execute(
                () -> {
                  calls.incrementAndGet();
                  throw response(429, "7");
                }));
    assertThat(executor.delays)
        .containsExactly(Duration.ofSeconds(7), Duration.ofSeconds(7), Duration.ofSeconds(7));
  }

  private static RestClientResponseException response(int status, String retryAfter) {
    HttpHeaders headers = new HttpHeaders();
    if (retryAfter != null) headers.set("Retry-After", retryAfter);
    return status >= 500
        ? HttpServerErrorException.create(
            HttpStatusCode.valueOf(status), "error", headers, new byte[0], null)
        : HttpClientErrorException.create(
            HttpStatusCode.valueOf(status), "error", headers, new byte[0], null);
  }

  static class TestExecutor extends ExternalApiRetryExecutor {
    final List<Duration> delays = new ArrayList<>();

    @Override
    protected void sleep(Duration delay) {
      delays.add(delay);
    }
  }
}
