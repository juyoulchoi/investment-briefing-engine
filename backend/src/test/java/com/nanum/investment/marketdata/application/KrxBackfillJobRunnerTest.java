package com.nanum.investment.marketdata.application;

import static org.mockito.Mockito.*;

import com.nanum.investment.marketdata.infrastructure.KrxBackfillRepository;
import com.nanum.investment.marketdata.infrastructure.KrxCollectionJobRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KrxBackfillJobRunnerTest {
  @Test
  void skipsDateAsUnconfirmedHolidayCandidateWhenEveryDatasetReturnsNoData() {
    KrxBackfillRepository backfills = mock(KrxBackfillRepository.class);
    KrxCollectionJobRepository dailyJobs = mock(KrxCollectionJobRepository.class);
    KrxCollectionJobRunner dailyRunner = mock(KrxCollectionJobRunner.class);
    KrxBackfillJobRunner runner = new KrxBackfillJobRunner(backfills, dailyJobs, dailyRunner);
    UUID backfillJobId = UUID.randomUUID();
    LocalDate baseDate = LocalDate.of(2026, 9, 4);
    var day =
        new KrxBackfillRepository.BackfillDayView(
            1L, baseDate, "PENDING", null, null, null, List.of(), 0, null, null, null);
    when(backfills.markRunning(backfillJobId)).thenReturn(true);
    when(backfills.find(backfillJobId)).thenReturn(job(backfillJobId));
    when(backfills.status(backfillJobId)).thenReturn("RUNNING");
    when(backfills.nextPending(backfillJobId))
        .thenReturn(Optional.of(day))
        .thenReturn(Optional.empty());
    when(dailyRunner.runNow(any(), eq(baseDate), anyList(), eq(250L)))
        .thenReturn(
            new KrxCollectionJobRepository.JobView(
                UUID.randomUUID(),
                baseDate,
                "COMPLETED_WITH_ERRORS",
                2,
                0,
                2,
                null,
                null,
                null,
                List.of(
                    item("KOSPI_STOCK_DAILY", "NO_DATA_UNEXPECTED"),
                    item("KOSDAQ_STOCK_DAILY", "NO_DATA_UNEXPECTED"))));

    runner.run(backfillJobId);

    verify(backfills)
        .finishDaySkipped(
            1L, "전체 요청 Dataset 빈 응답 - 휴장 후보(시장 달력 미확정)");
    verify(backfills, never()).finishDay(eq(1L), anyBoolean(), any());
    verify(backfills).complete(backfillJobId);
  }

  private KrxBackfillRepository.BackfillJobView job(UUID id) {
    return new KrxBackfillRepository.BackfillJobView(
        id,
        LocalDate.of(2026, 9, 4),
        LocalDate.of(2026, 9, 4),
        "RUNNING",
        List.of("KOSPI_STOCK_DAILY", "KOSDAQ_STOCK_DAILY"),
        250,
        1,
        0,
        0,
        0,
        1,
        null,
        null,
        null,
        null,
        null,
        List.of());
  }

  private KrxCollectionJobRepository.ItemView item(String dataset, String status) {
    return new KrxCollectionJobRepository.ItemView(dataset, status, 0, 0, null, null, null);
  }
}
