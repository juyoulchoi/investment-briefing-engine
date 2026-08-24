package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.*;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class FredMacroJobRunner {
  private final FredMacroRepository repository;
  private final FredRequestRateLimiter limiter;
  private final ObjectProvider<FredMacroService> services;

  public FredMacroJobRunner(
      FredMacroRepository repository,
      FredRequestRateLimiter limiter,
      ObjectProvider<FredMacroService> services) {
    this.repository = repository;
    this.limiter = limiter;
    this.services = services;
  }

  @Async("fredCollectorExecutor")
  public void run(UUID jobId) {
    try {
      if (!repository.markRunning(jobId)) return;
      while (true) {
        String status = repository.jobStatus(jobId);
        if ("PAUSE_REQUESTED".equals(status)) {
          repository.markPaused(jobId);
          return;
        }
        if ("CANCEL_REQUESTED".equals(status)) {
          repository.cancel(jobId);
          return;
        }
        var pending = repository.nextPending(jobId);
        if (pending.isEmpty()) {
          repository.complete(jobId);
          return;
        }
        var item = pending.get();
        repository.markItemRunning(item.itemId());
        try {
          limiter.acquire(item.requestIntervalMillis());
          repository.finishItem(item.itemId(), services.getObject().collectItem(item, jobId));
        } catch (RuntimeException error) {
          repository.failItem(item.itemId(), error.getMessage());
        }
        repository.updateProgress(jobId);
      }
    } catch (RuntimeException error) {
      repository.failJob(jobId, error.getMessage());
    }
  }
}
