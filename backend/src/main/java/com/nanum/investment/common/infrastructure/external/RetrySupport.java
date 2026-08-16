package com.nanum.investment.common.infrastructure.external;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class RetrySupport {
  public <T> T execute(Callable<T> action, RetryPolicy policy, Predicate<Throwable> retryable) {
    Throwable last = null;
    for (int attempt = 1; attempt <= policy.maximumAttempts(); attempt++)
      try {
        return action.call();
      } catch (Throwable ex) {
        last = ex;
        if (attempt == policy.maximumAttempts() || !retryable.test(ex)) throw propagate(ex);
        sleep(backoff(policy, attempt));
      }
    throw propagate(last);
  }

  Duration backoff(RetryPolicy p, int attempt) {
    long factor = 1L << Math.min(attempt - 1, 20);
    return p.initialDelay().multipliedBy(factor).compareTo(p.maximumDelay()) > 0
        ? p.maximumDelay()
        : p.initialDelay().multipliedBy(factor);
  }

  protected void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("재시도 대기가 중단되었습니다.", e);
    }
  }

  private RuntimeException propagate(Throwable e) {
    return e instanceof RuntimeException r ? r : new IllegalStateException(e);
  }
}
