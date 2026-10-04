package com.nanum.investment.marketdata.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanum.investment.marketdata.application.KofiaCollectionService;
import com.nanum.investment.marketdata.application.KofiaLookupService;
import com.nanum.investment.marketdata.infrastructure.KofiaRepository.JobView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class KofiaCollectionSchedulerTest {
  @Test
  void startsOverlappingCollectionForScheduledDatasets() {
    KofiaCollectionService service = mock(KofiaCollectionService.class);
    KofiaLookupService lookupService = mock(KofiaLookupService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-09-25T23:20:00Z"), ZoneId.of("Asia/Seoul"));
    KofiaCollectionScheduler scheduler =
        new KofiaCollectionScheduler(service, lookupService, 21, clock);
    JobView job = mock(JobView.class);
    when(job.jobId()).thenReturn(UUID.randomUUID());
    when(service.startJob(
            eq(java.time.LocalDate.of(2026, 9, 5)),
            eq(java.time.LocalDate.of(2026, 9, 25)),
            eq(KofiaCollectionScheduler.DAILY_DATASETS)))
        .thenReturn(job);

    scheduler.collectDailyMarketData();

    verify(service)
        .startJob(
            java.time.LocalDate.of(2026, 9, 5),
            java.time.LocalDate.of(2026, 9, 25),
            KofiaCollectionScheduler.DAILY_DATASETS);
    verify(lookupService).collectAll();

    assertThat(KofiaCollectionScheduler.DAILY_DATASETS)
        .containsExactly(
            "CREDIT_BALANCE_TREND",
            "SECURITIES_LENDING_TREND",
            "MARKET_FUNDS_TREND",
            "SECURITIES_LENDING_DETAILS",
            "CMA_DAILY_STATUS",
            "CMA_BALANCE_TREND",
            "OTC_INVESTOR_TRADING",
            "FINAL_QUOTED_YIELD",
            "KOSPI_MARKET",
            "KOSDAQ_MARKET");
  }

  @Test
  void usesConfiguredMondayThroughSaturdayMorningSchedule() throws Exception {
    Scheduled scheduled =
        KofiaCollectionScheduler.class
            .getMethod("collectDailyMarketData")
            .getAnnotation(Scheduled.class);

    assertThat(scheduled.cron()).isEqualTo("${kofia.scheduler.cron:0 20 8 * * MON-SAT}");
    assertThat(scheduled.zone()).isEqualTo("${kofia.scheduler.zone:Asia/Seoul}");
  }
}
