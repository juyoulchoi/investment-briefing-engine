package com.nanum.investment.common.infrastructure.external;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public record ExternalRetryPolicy(
    int maximumAttempts, List<Duration> delays, double jitterMinimum, double jitterMaximum,
    Set<Integer> retryableStatuses) {
  public ExternalRetryPolicy {
    if (maximumAttempts < 1) throw new IllegalArgumentException("maximumAttempts must be positive");
    delays = List.copyOf(delays);
    retryableStatuses = Set.copyOf(retryableStatuses);
  }

  public static ExternalRetryPolicy defaults() {
    return new ExternalRetryPolicy(
        4,
        List.of(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(10)),
        0.8,
        1.2,
        Set.of(408, 429, 500, 502, 503, 504));
  }
}
