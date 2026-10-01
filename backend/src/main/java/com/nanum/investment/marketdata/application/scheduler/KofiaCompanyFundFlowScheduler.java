package com.nanum.investment.marketdata.application.scheduler;

import com.nanum.investment.marketdata.application.KofiaCompanyFundFlowService;
import com.nanum.investment.marketdata.domain.KofiaCompanyFundFlowVariant.Stage;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kofia.company-fund-flow.scheduler.enabled", havingValue = "true")
public class KofiaCompanyFundFlowScheduler {
  private final KofiaCompanyFundFlowService service;

  @Scheduled(
      cron = "${kofia.company-fund-flow.scheduler.cron:0 20 1 * * TUE-SAT}",
      zone = "${kofia.company-fund-flow.scheduler.zone:Asia/Seoul}")
  public void collectPreviousBusinessDate() {
    LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    try {
      var job = service.startJob(Stage.ALL_ACTIVE, date, date);
      log.info("KOFIA 회사별자금유출입 일간 수집 Job 생성. jobId={}, date={}", job.jobId(), date);
    } catch (RuntimeException error) {
      log.error("KOFIA 회사별자금유출입 일간 수집 Job 생성 실패", error);
    }
  }
}
