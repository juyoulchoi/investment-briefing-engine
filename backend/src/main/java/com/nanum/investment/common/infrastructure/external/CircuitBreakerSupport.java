package com.nanum.investment.common.infrastructure.external;

import java.time.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerSupport {
  private final ConcurrentMap<String, State> states = new ConcurrentHashMap<>();

  public <T> T execute(
      String key, int failureThreshold, Duration openDuration, Supplier<T> action) {
    State s = states.computeIfAbsent(key, k -> new State());
    synchronized (s) {
      if (s.openedAt != null && Instant.now().isBefore(s.openedAt.plus(openDuration)))
        throw new CircuitOpenException(key);
      if (s.openedAt != null) s.openedAt = null;
    }
    try {
      T value = action.get();
      s.failures = 0;
      return value;
    } catch (RuntimeException ex) {
      s.failures++;
      if (s.failures >= failureThreshold) s.openedAt = Instant.now();
      throw ex;
    }
  }

  public boolean isOpen(String key, Duration duration) {
    State s = states.get(key);
    return s != null && s.openedAt != null && Instant.now().isBefore(s.openedAt.plus(duration));
  }

  private static final class State {
    volatile int failures;
    volatile Instant openedAt;
  }

  public static class CircuitOpenException extends RuntimeException {
    public CircuitOpenException(String key) {
      super("Circuit breaker is open: " + key);
    }
  }
}
