package com.nanum.investment.marketdata.application.scheduler;

import com.nanum.investment.marketdata.application.KofiaFundFlowService;
import com.nanum.investment.marketdata.domain.KofiaFundFlowVariant.Stage;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kofia.fund-flow.scheduler.enabled", havingValue = "true")
public class KofiaFundFlowScheduler {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private final KofiaFundFlowService service;

  public KofiaFundFlowScheduler(KofiaFundFlowService service) {
    this.service = service;
  }

  @Scheduled(
      cron = "${kofia.fund-flow.scheduler.cron:0 20 0 * * TUE-SAT}",
      zone = "${kofia.fund-flow.scheduler.zone:Asia/Seoul}")
  public void collectActiveVariants() {
    LocalDate to = LocalDate.now(SEOUL);
    LocalDate from = to.minusDays(20);
    try {
      var job = service.startJob(Stage.ALL_ACTIVE, from, to);
      log.info(
          "KOFIA 기간자금유출입 활성 조합 중첩 수집 Job 생성. jobId={}, from={}, to={}, items={}",
          job.jobId(),
          from,
          to,
          job.totalItemCount());
    } catch (Exception error) {
      log.error("KOFIA 기간자금유출입 활성 조합 중첩 수집 Job 생성 실패.", error);
    }
  }
}
