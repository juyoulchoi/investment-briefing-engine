package com.nanum.investment.common.infrastructure.external;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpTimeoutException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.springframework.web.client.*;

public final class ExternalHttpRetry {
  private static final Set<Integer> RETRYABLE = Set.of(408, 429, 500, 502, 503, 504);

  private ExternalHttpRetry() {}

  public static boolean isRetryable(Throwable error) {
    if (error instanceof RestClientResponseException response)
      return RETRYABLE.contains(response.getStatusCode().value());
    if (!(error instanceof ResourceAccessException)) return false;
    for (Throwable cause = error; cause != null; cause = cause.getCause())
      if (cause instanceof SocketTimeoutException
          || cause instanceof HttpTimeoutException
          || cause instanceof ConnectException
          || cause instanceof SocketException
          || cause instanceof IOException) return true;
    return false;
  }

  public static Duration retryAfter(Throwable error) {
    if (!(error instanceof RestClientResponseException response)
        || response.getStatusCode().value() != 429) return null;
    String value =
        response.getResponseHeaders() == null
            ? null
            : response.getResponseHeaders().getFirst("Retry-After");
    if (value == null || value.isBlank()) return null;
    try {
      return Duration.ofSeconds(Math.max(0, Long.parseLong(value.trim())));
    } catch (NumberFormatException ignored) {
    }
    try {
      Duration delay =
          Duration.between(
              Instant.now(),
              ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
      return delay.isNegative() ? Duration.ZERO : delay;
    } catch (Exception ignored) {
      return null;
    }
  }
}
