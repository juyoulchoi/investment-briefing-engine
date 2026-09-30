package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KofiaFundFlowRecovery {
  private final KofiaFundFlowRepository repository;
  private final KofiaFundFlowJobRunner runner;

  public KofiaFundFlowRecovery(KofiaFundFlowRepository repository, KofiaFundFlowJobRunner runner) {
    this.repository = repository;
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void resumeQueuedJobs() {
    var jobIds = repository.recoverQueuedJobs();
    if (!jobIds.isEmpty()) log.info("KOFIA 기간자금유출입 대기 Job 자동 복구. jobIds={}", jobIds);
    jobIds.forEach(runner::run);
  }
}
