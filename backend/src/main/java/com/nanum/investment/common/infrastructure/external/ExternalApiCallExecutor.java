package com.nanum.investment.common.infrastructure.external;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ExternalApiCallExecutor {
  private final ExternalApiRetryExecutor retry;
  private final ExternalApiLogService logs;

  public ExternalApiCallExecutor(ExternalApiRetryExecutor retry, ExternalApiLogService logs) {
    this.retry = retry;
    this.logs = logs;
  }

  public <T> T execute(Call call, Callable<T> request) {
    OffsetDateTime requestedAt = OffsetDateTime.now();
    AtomicInteger attempts = new AtomicInteger();
    try {
      T response = retry.execute(call.policyKey(), () -> {
        attempts.incrementAndGet();
        return request.call();
      });
      logs.save(UUID.randomUUID().toString(), call.group(), call.name(), call.method(), call.url(),
          call.requestBody(), 200, response == null ? null : response.toString(), true,
          Math.max(0, attempts.get() - 1), requestedAt, null);
      return response;
    } catch (RuntimeException error) {
      Integer status = error instanceof RestClientResponseException response
          ? response.getStatusCode().value() : null;
      String responseBody = error instanceof RestClientResponseException response
          ? response.getResponseBodyAsString() : null;
      logs.save(UUID.randomUUID().toString(), call.group(), call.name(), call.method(), call.url(),
          call.requestBody(), status, responseBody, false, Math.max(0, attempts.get() - 1),
          requestedAt, error.getMessage());
      throw error;
    }
  }

  public record Call(
      String policyKey, String group, String name, String method, String url, String requestBody) {}
}
