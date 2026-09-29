package com.nanum.investment.marketdata.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanum.investment.marketdata.application.KofiaCollectionService;
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
    Clock clock = Clock.fixed(Instant.parse("2026-09-25T14:40:00Z"), ZoneId.of("Asia/Seoul"));
    KofiaCollectionScheduler scheduler = new KofiaCollectionScheduler(service, 21, clock);
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

    assertThat(KofiaCollectionScheduler.DAILY_DATASETS)
        .containsExactly(
            "CREDIT_BALANCE_TREND",
            "SECURITIES_LENDING_TREND",
            "MARKET_FUNDS_TREND",
            "SECURITIES_LENDING_DETAILS",
            "CMA_DAILY_STATUS",
            "CMA_BALANCE_TREND",
            "FUND_FLOW_PERIOD",
            "CUSTOMER_TYPE_FUND_SCALE_PERIOD",
            "ASSET_MANAGER_FUND_FLOW",
            "OTC_INVESTOR_TRADING",
            "FINAL_QUOTED_YIELD",
            "KOSPI_MARKET",
            "KOSDAQ_MARKET");
  }

  @Test
  void usesConfiguredWeekdayLateNightSchedule() throws Exception {
    Scheduled scheduled =
        KofiaCollectionScheduler.class
            .getMethod("collectDailyMarketData")
            .getAnnotation(Scheduled.class);

    assertThat(scheduled.cron()).isEqualTo("${kofia.scheduler.cron:0 40 23 * * MON-FRI}");
    assertThat(scheduled.zone()).isEqualTo("${kofia.scheduler.zone:Asia/Seoul}");
  }
}
