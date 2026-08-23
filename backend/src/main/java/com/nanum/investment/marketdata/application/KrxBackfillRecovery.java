package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.KrxBackfillRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class KrxBackfillRecovery {
  private final KrxBackfillRepository backfills;
  private final KrxBackfillJobRunner runner;

  public KrxBackfillRecovery(KrxBackfillRepository backfills, KrxBackfillJobRunner runner) {
    this.backfills = backfills;
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void recover() {
    backfills.recoverInterrupted().forEach(runner::run);
  }
}
