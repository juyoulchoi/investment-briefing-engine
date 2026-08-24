package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.infrastructure.FredMacroRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FredMacroRecovery {
  private final FredMacroRepository repository;
  private final FredMacroJobRunner runner;

  public FredMacroRecovery(FredMacroRepository repository, FredMacroJobRunner runner) {
    this.repository = repository;
    this.runner = runner;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void recover() {
    repository.recoverInterrupted().forEach(runner::run);
  }
}
