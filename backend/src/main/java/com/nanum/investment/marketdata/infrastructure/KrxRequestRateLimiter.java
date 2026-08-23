package com.nanum.investment.marketdata.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KrxRequestRateLimiter {
  private final long defaultIntervalMillis;
  private long nextRequestAt;

  public KrxRequestRateLimiter(
      @Value("${krx.collection.min-request-interval:250ms}") java.time.Duration defaultInterval) {
    defaultIntervalMillis = defaultInterval.toMillis();
  }

  public void acquire() {
    acquire(defaultIntervalMillis);
  }

  public synchronized void acquire(long requestedIntervalMillis) {
    long interval = Math.max(defaultIntervalMillis, requestedIntervalMillis);
    long waitMillis = Math.max(0, nextRequestAt - System.currentTimeMillis());
    if (waitMillis > 0) sleep(waitMillis);
    nextRequestAt = System.currentTimeMillis() + interval;
  }

  private void sleep(long waitMillis) {
    try {
      Thread.sleep(waitMillis);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("KRX 요청 간격 대기가 중단되었습니다.", exception);
    }
  }
}
