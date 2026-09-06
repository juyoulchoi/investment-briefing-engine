package com.nanum.investment.marketdata.infrastructure;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class YahooRequestRateLimiter {
  private final long intervalMillis;
  private long nextRequestAt;

  public YahooRequestRateLimiter(
      @Value("${overseas.yahoo.collection.min-request-interval:250ms}") Duration interval) {
    this.intervalMillis = Math.max(0, interval.toMillis());
  }

  public synchronized void acquire() {
    long wait = Math.max(0, nextRequestAt - System.currentTimeMillis());
    if (wait > 0)
      try {
        Thread.sleep(wait);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Yahoo 요청 간격 대기가 중단되었습니다.", error);
      }
    nextRequestAt = System.currentTimeMillis() + intervalMillis;
  }
}
