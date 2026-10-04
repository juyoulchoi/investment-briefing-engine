package com.nanum.investment.marketdata.application.scheduler;

import com.nanum.investment.marketdata.application.KofiaCompanyFundFlowService;
import com.nanum.investment.marketdata.domain.KofiaCompanyFundFlowVariant.Stage;
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
@ConditionalOnProperty(name = "kofia.company-fund-flow.scheduler.enabled", havingValue = "true")
public class KofiaCompanyFundFlowScheduler {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private final KofiaCompanyFundFlowService service;
  private final Clock clock;

  @Autowired
  public KofiaCompanyFundFlowScheduler(KofiaCompanyFundFlowService service) {
    this(service, Clock.system(SEOUL));
  }

  KofiaCompanyFundFlowScheduler(KofiaCompanyFundFlowService service, Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  @Scheduled(
      cron = "${kofia.company-fund-flow.scheduler.cron:0 20 8 * * MON-SAT}",
      zone = "${kofia.company-fund-flow.scheduler.zone:Asia/Seoul}")
  public void collectPreviousBusinessDate() {
    LocalDate date = LocalDate.now(clock).minusDays(1);
    while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
        || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) date = date.minusDays(1);
    try {
      var job = service.startJob(Stage.ALL_ACTIVE, date, date);
      log.info("KOFIA 회사별자금유출입 일간 수집 Job 생성. jobId={}, date={}", job.jobId(), date);
    } catch (RuntimeException error) {
      log.error("KOFIA 회사별자금유출입 일간 수집 Job 생성 실패", error);
    }
  }
}
