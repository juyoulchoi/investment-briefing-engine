package com.nanum.investment.common.infrastructure.external;

import java.time.Duration;
import java.util.*;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ExternalRetryPolicyResolver {
  private final Environment environment;

  public ExternalRetryPolicyResolver(Environment environment) {
    this.environment = environment;
  }

  public ExternalRetryPolicy resolve(String policyKey) {
    ExternalRetryPolicy defaults = ExternalRetryPolicy.defaults();
    String[] parts = policyKey.toLowerCase(Locale.ROOT).split("\\.", 2);
    String provider = "external-api.providers." + parts[0] + ".retry.";
    String dataset =
        parts.length == 2
            ? "external-api.providers." + parts[0] + ".datasets." + parts[1] + ".retry."
            : null;
    return new ExternalRetryPolicy(
        integer(dataset, provider, "maximum-attempts", defaults.maximumAttempts()),
        durations(dataset, provider, "backoff", defaults.delays()),
        decimal(dataset, provider, "jitter-minimum", defaults.jitterMinimum()),
        decimal(dataset, provider, "jitter-maximum", defaults.jitterMaximum()),
        integers(dataset, provider, "retryable-statuses", defaults.retryableStatuses()));
  }

  private String value(String dataset, String provider, String name) {
    String result = dataset == null ? null : environment.getProperty(dataset + name);
    return result != null ? result : environment.getProperty(provider + name);
  }

  private int integer(String d, String p, String n, int fallback) {
    String value = value(d, p, n);
    return value == null ? fallback : Integer.parseInt(value.trim());
  }

  private double decimal(String d, String p, String n, double fallback) {
    String value = value(d, p, n);
    return value == null ? fallback : Double.parseDouble(value.trim());
  }

  private List<Duration> durations(String d, String p, String n, List<Duration> fallback) {
    String value = value(d, p, n);
    return value == null
        ? fallback
        : Arrays.stream(value.split(","))
            .map(String::trim)
            .map(DurationStyle::detectAndParse)
            .toList();
  }

  private Set<Integer> integers(String d, String p, String n, Set<Integer> fallback) {
    String value = value(d, p, n);
    if (value == null) return fallback;
    Set<Integer> result = new LinkedHashSet<>();
    Arrays.stream(value.split(",")).map(String::trim).map(Integer::valueOf).forEach(result::add);
    return result;
  }
}
