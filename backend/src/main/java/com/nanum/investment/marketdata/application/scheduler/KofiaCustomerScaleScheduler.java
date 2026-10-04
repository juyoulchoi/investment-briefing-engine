package com.nanum.investment.marketdata.application.scheduler;

import com.nanum.investment.marketdata.application.KofiaCustomerScaleService;
import com.nanum.investment.marketdata.domain.KofiaCustomerScaleVariant.Stage;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kofia.customer-scale.scheduler.enabled", havingValue = "true")
public class KofiaCustomerScaleScheduler {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private final KofiaCustomerScaleService service;
  private final Clock clock;

  @Autowired
  public KofiaCustomerScaleScheduler(KofiaCustomerScaleService service) {
    this(service, Clock.system(SEOUL));
  }

  KofiaCustomerScaleScheduler(KofiaCustomerScaleService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  @Scheduled(
      cron = "${kofia.customer-scale.scheduler.cron:0 20 8 * * MON-SAT}",
      zone = "${kofia.customer-scale.scheduler.zone:Asia/Seoul}")
  public void collectLatestMonthEnd() {
    LocalDate previousMonth = LocalDate.now(clock).minusMonths(1);
    LocalDate from = previousMonth.withDayOfMonth(1);
    LocalDate to = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());
    try {
      var job = service.startJob(Stage.ALL_ACTIVE, from, to);
      log.info(
          "KOFIA 고객유형별규모 최신 월말 조건별 일일 수집 Job 생성. jobId={}, from={}, to={}", job.jobId(), from, to);
    } catch (RuntimeException error) {
      log.error("KOFIA 고객유형별규모 최신 월말 조건별 일일 수집 Job 생성 실패", error);
    }
  }
}
