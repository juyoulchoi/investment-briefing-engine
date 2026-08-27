package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.KofiaRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaRequestRateLimiter;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class KofiaCollectionJobRunner {
  private final KofiaRepository repository;
  private final KofiaRequestRateLimiter limiter;
  private final ObjectProvider<KofiaCollectionService> services;

  public KofiaCollectionJobRunner(
      KofiaRepository repository,
      KofiaRequestRateLimiter limiter,
      ObjectProvider<KofiaCollectionService> services) {
    this.repository = repository;
    this.limiter = limiter;
    this.services = services;
  }

  @Async("kofiaCollectorExecutor")
  public void run(UUID jobId) {
    try {
      if (!repository.markRunning(jobId)) return;
      while (true) {
        KofiaRepository.PendingItem item = repository.nextPending(jobId);
        if (item == null) {
          repository.complete(jobId);
          return;
        }
        repository.markItemRunning(item.itemId());
        try {
          limiter.acquire(0);
          var result = services.getObject().collect(item.dataset(), item.from(), item.to(), jobId);
          repository.finishItem(item.itemId(), result.storedCount());
        } catch (RuntimeException error) {
          repository.failItem(item.itemId(), error.getMessage());
        }
      }
    } catch (RuntimeException error) {
      repository.failJob(jobId, error.getMessage());
    }
  }
}
