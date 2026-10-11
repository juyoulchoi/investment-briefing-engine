package com.nanum.collector;

import com.nanum.investment.marketdata.application.FredMacroJobRunner;
import com.nanum.investment.marketdata.application.KrxBackfillJobRunner;
import com.nanum.investment.marketdata.infrastructure.FredMacroRepository;
import com.nanum.investment.marketdata.infrastructure.KrxBackfillRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="MARKET_RECOVERY_ENABLED", havingValue="true")
public class CollectorMarketRecovery {
  private final FredMacroRepository fred;
  private final FredMacroJobRunner fredRunner;
  private final KrxBackfillRepository krx;
  private final KrxBackfillJobRunner krxRunner;
  public CollectorMarketRecovery(FredMacroRepository fred, FredMacroJobRunner fredRunner,
      KrxBackfillRepository krx, KrxBackfillJobRunner krxRunner) {
    this.fred=fred;
    this.fredRunner=fredRunner;
    this.krx=krx;
    this.krxRunner=krxRunner;
  }
  @EventListener(ApplicationReadyEvent.class)
  public void recover() {
    fred.recoverInterrupted().forEach(fredRunner::run);
    krx.recoverInterrupted().forEach(krxRunner::run);
  }
}
