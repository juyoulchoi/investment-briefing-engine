package com.nanum.investment.marketdata.infrastructure;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KofiaRequestRateLimiter {
  private final long configuredIntervalMillis;
  private long lastRequestMillis;

  public KofiaRequestRateLimiter(
      @Value("${kofia.collection.min-request-interval:250ms}") Duration interval) {
    configuredIntervalMillis = Math.max(0, interval.toMillis());
  }

  public synchronized void acquire(long requestedIntervalMillis) {
    long interval = Math.max(configuredIntervalMillis, requestedIntervalMillis);
    long wait = lastRequestMillis + interval - System.currentTimeMillis();
    if (wait > 0) {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("KOFIA 호출 대기가 중단되었습니다.", error);
      }
    }
    lastRequestMillis = System.currentTimeMillis();
  }
}
