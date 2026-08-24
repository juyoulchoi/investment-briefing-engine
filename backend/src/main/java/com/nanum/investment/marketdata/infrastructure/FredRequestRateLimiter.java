package com.nanum.investment.marketdata.infrastructure;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FredRequestRateLimiter {
  private final long minimumIntervalMillis;
  private final AtomicLong nextAllowedAt = new AtomicLong();

  public FredRequestRateLimiter(
      @Value("${fred.collection.min-request-interval:250ms}") Duration minimumInterval) {
    this.minimumIntervalMillis = Math.max(0, minimumInterval.toMillis());
  }

  public synchronized void acquire(long requestedIntervalMillis) {
    long interval = Math.max(minimumIntervalMillis, requestedIntervalMillis);
    long now = System.currentTimeMillis();
    long wait = Math.max(0, nextAllowedAt.get() - now);
    if (wait > 0) {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("FRED 호출 대기가 중단되었습니다.", error);
      }
    }
    nextAllowedAt.set(System.currentTimeMillis() + interval);
  }
}
