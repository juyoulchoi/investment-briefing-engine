package com.nanum.investment.common.infrastructure.external;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExternalApiRetryExecutor {
  private final ExternalRetryPolicyResolver policies;

  protected ExternalApiRetryExecutor() {
    this.policies = null;
  }

  @Autowired
  public ExternalApiRetryExecutor(ExternalRetryPolicyResolver policies) {
    this.policies = policies;
  }

  public <T> T execute(Callable<T> action) {
    return execute("default", action);
  }

  public <T> T execute(String policyKey, Callable<T> action) {
    ExternalRetryPolicy policy = policies == null ? ExternalRetryPolicy.defaults() : policies.resolve(policyKey);
    Throwable last = null;
    for (int attempt = 1; attempt <= policy.maximumAttempts(); attempt++)
      try {
        return action.call();
      } catch (Throwable error) {
        last = error;
        if (attempt == policy.maximumAttempts()
            || !ExternalHttpRetry.isRetryable(error, policy.retryableStatuses()))
          throw propagate(error);
        Duration retryAfter = ExternalHttpRetry.retryAfter(error);
        Duration base = policy.delays().get(Math.min(attempt - 1, policy.delays().size() - 1));
        sleep(retryAfter != null ? retryAfter : jitter(base, policy));
      }
    throw propagate(last);
  }

  Duration jitter(Duration base) {
    return jitter(base, ExternalRetryPolicy.defaults());
  }

  Duration jitter(Duration base, ExternalRetryPolicy policy) {
    double factor =
        ThreadLocalRandom.current().nextDouble(policy.jitterMinimum(), policy.jitterMaximum());
    return Duration.ofMillis(Math.round(base.toMillis() * factor));
  }

  protected void sleep(Duration delay) {
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("재시도 대기가 중단되었습니다.", error);
    }
  }

  private RuntimeException propagate(Throwable error) {
    return error instanceof RuntimeException runtime ? runtime : new IllegalStateException(error);
  }
}
