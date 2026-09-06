package com.nanum.investment.common.infrastructure.external;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExternalResilienceTest {
  @Test
  void retryStopsAfterSuccessAndUsesMaximumFourAttempts() {
    AtomicInteger calls = new AtomicInteger();
    RetrySupport retry =
        new RetrySupport() {
          @Override
          protected void sleep(Duration ignored) {}
        };
    String value =
        retry.execute(
            () -> {
              if (calls.incrementAndGet() < 4) throw new IllegalStateException("temporary");
              return "ok";
            },
            RetryPolicy.externalApiDefault(),
            e -> true);
    assertThat(value).isEqualTo("ok");
    assertThat(calls).hasValue(4);
  }

  @Test
  void nonRetryableFailureStopsImmediately() {
    AtomicInteger calls = new AtomicInteger();
    RetrySupport retry =
        new RetrySupport() {
          @Override
          protected void sleep(Duration ignored) {}
        };
    assertThatThrownBy(
            () ->
                retry.execute(
                    () -> {
                      calls.incrementAndGet();
                      throw new IllegalArgumentException("bad request");
                    },
                    RetryPolicy.externalApiDefault(),
                    e -> false))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(calls).hasValue(1);
  }

  @Test
  void circuitOpensAtFailureThreshold() {
    CircuitBreakerSupport circuit = new CircuitBreakerSupport();
    for (int i = 0; i < 3; i++)
      assertThatThrownBy(
          () ->
              circuit.execute(
                  "YAHOO",
                  3,
                  Duration.ofMinutes(1),
                  () -> {
                    throw new IllegalStateException("down");
                  }));
    assertThat(circuit.isOpen("YAHOO", Duration.ofMinutes(1))).isTrue();
    assertThatThrownBy(() -> circuit.execute("YAHOO", 3, Duration.ofMinutes(1), () -> "never"))
        .isInstanceOf(CircuitBreakerSupport.CircuitOpenException.class);
  }

  @Test
  void circuitAllowsOneHalfOpenProbeAndClosesAfterSuccess() {
    CircuitBreakerSupport circuit = new CircuitBreakerSupport();
    assertThatThrownBy(
        () ->
            circuit.execute(
                "KRX",
                1,
                Duration.ZERO,
                () -> {
                  throw new IllegalStateException("down");
                }));

    assertThat(circuit.execute("KRX", 1, Duration.ZERO, () -> "recovered")).isEqualTo("recovered");
    assertThat(circuit.isOpen("KRX", Duration.ZERO)).isFalse();
  }

  @Test
  void apiLogMasksSecretsAndLimitsSize() {
    ApiLogMasker masker = new ApiLogMasker();
    String masked =
        masker.maskAndLimit("apiKey=secret authorization:BearerToken accountNumber=123456");
    assertThat(masked)
        .doesNotContain("secret", "BearerToken", "123456")
        .contains("apiKey=***", "authorization:***", "accountNumber=***");
    assertThat(masker.maskAndLimit("x".repeat(9000))).hasSizeLessThan(8100).endsWith("[TRUNCATED]");
  }
}
