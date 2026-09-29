package com.nanum.investment.marketdata.application.scheduler;

import com.nanum.investment.marketdata.application.KofiaCollectionService;
import com.nanum.investment.marketdata.infrastructure.KofiaRepository.JobView;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "kofia.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class KofiaCollectionScheduler {
  static final List<String> DAILY_DATASETS =
      List.of(
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
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final KofiaCollectionService service;
  private final int overlapDays;
  private final Clock clock;

  @Autowired
  public KofiaCollectionScheduler(
      KofiaCollectionService service,
      @Value("${kofia.scheduler.overlap-days:21}") int overlapDays) {
    this(service, overlapDays, Clock.system(SEOUL));
  }

  KofiaCollectionScheduler(KofiaCollectionService service, int overlapDays, Clock clock) {
    if (overlapDays < 1) throw new IllegalArgumentException("KOFIA 중첩 수집일은 1일 이상이어야 합니다.");
    this.service = service;
    this.overlapDays = overlapDays;
    this.clock = clock;
  }

  @Scheduled(
      cron = "${kofia.scheduler.cron:0 40 23 * * MON-FRI}",
      zone = "${kofia.scheduler.zone:Asia/Seoul}")
  public void collectDailyMarketData() {
    LocalDate to = LocalDate.now(clock);
    LocalDate from = to.minusDays(overlapDays - 1L);
    try {
      JobView job = service.startJob(from, to, DAILY_DATASETS);
      log.info(
          "KOFIA 일일 중첩 수집 Job 생성. jobId={}, from={}, to={}, datasets={}",
          job.jobId(),
          from,
          to,
          DAILY_DATASETS);
    } catch (Exception error) {
      log.error(
          "KOFIA 일일 중첩 수집 Job 생성 실패. from={}, to={}, datasets={}", from, to, DAILY_DATASETS, error);
    }
  }
}
