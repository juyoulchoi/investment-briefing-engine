package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.domain.KrxDataset;
import com.nanum.investment.marketdata.infrastructure.KrxBackfillRepository;
import com.nanum.investment.marketdata.infrastructure.KrxCollectionJobRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class KrxBackfillJobRunner {
  private final KrxBackfillRepository backfills;
  private final KrxCollectionJobRepository dailyJobs;
  private final KrxCollectionJobRunner dailyRunner;

  public KrxBackfillJobRunner(
      KrxBackfillRepository backfills,
      KrxCollectionJobRepository dailyJobs,
      KrxCollectionJobRunner dailyRunner) {
    this.backfills = backfills;
    this.dailyJobs = dailyJobs;
    this.dailyRunner = dailyRunner;
  }

  @Async("krxCollectorExecutor")
  public void run(UUID backfillJobId) {
    try {
      if (!backfills.markRunning(backfillJobId)) return;
      KrxBackfillRepository.BackfillJobView job = backfills.find(backfillJobId);
      while (true) {
        String control = backfills.status(backfillJobId);
        if ("PAUSE_REQUESTED".equals(control)) {
          backfills.markPaused(backfillJobId);
          return;
        }
        if ("CANCEL_REQUESTED".equals(control)) {
          backfills.cancel(backfillJobId);
          return;
        }

        var next = backfills.nextPending(backfillJobId);
        if (next.isEmpty()) {
          backfills.complete(backfillJobId);
          return;
        }

        var day = next.get();
        List<KrxDataset> datasets =
            (day.retryDatasets().isEmpty() ? job.datasets() : day.retryDatasets())
                .stream().map(KrxDataset::valueOf).toList();
        UUID dailyJobId = UUID.randomUUID();
        dailyJobs.create(dailyJobId, day.baseDate(), datasets.size());
        backfills.markDayRunning(day.dayId(), dailyJobId);
        try {
          var result =
              dailyRunner.runNow(dailyJobId, day.baseDate(), datasets, job.requestIntervalMillis());
          boolean success = result.failedCount() == 0;
          String error =
              success
                  ? null
                  : result.items().stream()
                      .filter(item -> !"SUCCESS".equals(item.status()))
                      .map(item -> item.dataset() + ": " + item.error())
                      .reduce((left, right) -> left + " | " + right)
                      .orElse("KRX 날짜별 수집 실패");
          backfills.finishDay(day.dayId(), success, error);
        } catch (RuntimeException exception) {
          backfills.finishDay(day.dayId(), false, exception.getMessage());
        }
        backfills.updateProgress(backfillJobId, day.baseDate());
      }
    } catch (RuntimeException exception) {
      backfills.fail(backfillJobId, exception.getMessage());
    }
  }
}
