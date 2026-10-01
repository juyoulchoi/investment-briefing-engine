package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.KofiaCompanyFundFlowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KofiaCompanyFundFlowRecovery {
  private final KofiaCompanyFundFlowRepository repository;
  private final KofiaCompanyFundFlowJobRunner runner;

  public KofiaCompanyFundFlowRecovery(
      KofiaCompanyFundFlowRepository repository, KofiaCompanyFundFlowJobRunner runner) {
    this.repository = repository;
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void resumeQueuedJobs() {
    var jobIds = repository.recoverQueuedJobs();
    if (!jobIds.isEmpty()) log.info("KOFIA 회사별자금유출입 대기 Job 자동 복구. jobIds={}", jobIds);
    jobIds.forEach(runner::run);
  }
}
