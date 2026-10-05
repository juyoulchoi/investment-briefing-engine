package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "kofia.recovery.enabled",
    havingValue = "true",
    matchIfMissing = true)
@Component
public class KofiaCustomerScaleRecovery {
  private final KofiaCustomerScaleRepository repository;
  private final KofiaCustomerScaleJobRunner runner;

  public KofiaCustomerScaleRecovery(
      KofiaCustomerScaleRepository repository, KofiaCustomerScaleJobRunner runner) {
    this.repository = repository;
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void resumeQueuedJobs() {
    var jobIds = repository.recoverQueuedJobs();
    if (!jobIds.isEmpty()) log.info("KOFIA 고객유형별규모 대기 Job 자동 복구. jobIds={}", jobIds);
    jobIds.forEach(runner::run);
  }
}
