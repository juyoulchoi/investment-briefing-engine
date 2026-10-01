package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.KofiaCompanyFundFlowRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaRequestRateLimiter;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class KofiaCompanyFundFlowJobRunner {
  private final KofiaCompanyFundFlowRepository repository;
  private final KofiaRequestRateLimiter limiter;
  private final ObjectProvider<KofiaCompanyFundFlowService> services;

  public KofiaCompanyFundFlowJobRunner(
      KofiaCompanyFundFlowRepository repository,
      KofiaRequestRateLimiter limiter,
      ObjectProvider<KofiaCompanyFundFlowService> services) {
    this.repository = repository;
    this.limiter = limiter;
    this.services = services;
  }

  @Async("kofiaCollectorExecutor")
  public void run(UUID jobId) {
    try {
      if (!repository.markRunning(jobId)) return;
      while (true) {
        KofiaCompanyFundFlowRepository.PendingItem item = repository.nextPending(jobId);
        if (item == null) {
          repository.complete(jobId);
          return;
        }
        repository.markItemRunning(item.itemId());
        try {
          limiter.acquire(0);
          var result = services.getObject().collect(item);
          repository.finishItem(item.itemId(), result);
        } catch (RuntimeException error) {
          repository.failItem(item.itemId(), error.getMessage());
        }
      }
    } catch (RuntimeException error) {
      repository.failJob(jobId, error.getMessage());
    }
  }
}
