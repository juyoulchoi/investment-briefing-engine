package com.nanum.investment.marketdata.application.scheduler;

import com.nanum.investment.marketdata.application.KofiaCustomerScaleService;
import com.nanum.investment.marketdata.domain.KofiaCustomerScaleVariant.Stage;
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
@ConditionalOnProperty(name = "kofia.customer-scale.scheduler.enabled", havingValue = "true")
public class KofiaCustomerScaleScheduler {
  private final KofiaCustomerScaleService service;

  @Scheduled(
      cron = "${kofia.customer-scale.scheduler.cron:0 40 0 10 * *}",
      zone = "${kofia.customer-scale.scheduler.zone:Asia/Seoul}")
  public void collectPreviousMonth() {
    LocalDate previousMonth = LocalDate.now(ZoneId.of("Asia/Seoul")).minusMonths(1);
    LocalDate from = previousMonth.withDayOfMonth(1);
    LocalDate to = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());
    try {
      var job = service.startJob(Stage.ALL_ACTIVE, from, to);
      log.info("KOFIA 고객유형별규모 월간 수집 Job 생성. jobId={}, from={}, to={}", job.jobId(), from, to);
    } catch (RuntimeException error) {
      log.error("KOFIA 고객유형별규모 월간 수집 Job 생성 실패", error);
    }
  }
}
