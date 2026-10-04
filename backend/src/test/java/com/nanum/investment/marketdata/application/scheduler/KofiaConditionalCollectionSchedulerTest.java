package com.nanum.investment.marketdata.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanum.investment.marketdata.application.KofiaCompanyFundFlowService;
import com.nanum.investment.marketdata.application.KofiaCustomerScaleService;
import com.nanum.investment.marketdata.application.KofiaFundFlowService;
import com.nanum.investment.marketdata.domain.KofiaCompanyFundFlowVariant;
import com.nanum.investment.marketdata.domain.KofiaCustomerScaleVariant;
import com.nanum.investment.marketdata.domain.KofiaFundFlowVariant;
import com.nanum.investment.marketdata.infrastructure.KofiaCompanyFundFlowRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class KofiaConditionalCollectionSchedulerTest {
  private static final Clock SATURDAY_CLOCK =
      Clock.fixed(Instant.parse("2026-10-02T23:20:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void fundFlowCollectsAllConditionsWithTwentyOneDayOverlap() throws Exception {
    KofiaFundFlowService service = mock(KofiaFundFlowService.class);
    KofiaFundFlowRepository.JobView job = mock(KofiaFundFlowRepository.JobView.class);
    when(job.jobId()).thenReturn(UUID.randomUUID());
    when(service.startJob(
            KofiaFundFlowVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 9, 12),
            LocalDate.of(2026, 10, 2)))
        .thenReturn(job);

    new KofiaFundFlowScheduler(service, SATURDAY_CLOCK).collectActiveVariants();

    verify(service)
        .startJob(
            KofiaFundFlowVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 9, 12),
            LocalDate.of(2026, 10, 2));
    assertWeekdaySchedule(KofiaFundFlowScheduler.class, "collectActiveVariants");
  }

  @Test
  void customerScaleRechecksLatestCompletedMonthForAllConditions() throws Exception {
    KofiaCustomerScaleService service = mock(KofiaCustomerScaleService.class);
    KofiaCustomerScaleRepository.JobView job = mock(KofiaCustomerScaleRepository.JobView.class);
    when(job.jobId()).thenReturn(UUID.randomUUID());
    when(service.startJob(
            KofiaCustomerScaleVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30)))
        .thenReturn(job);

    new KofiaCustomerScaleScheduler(service, SATURDAY_CLOCK).collectLatestMonthEnd();

    verify(service)
        .startJob(
            KofiaCustomerScaleVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30));
    assertWeekdaySchedule(KofiaCustomerScaleScheduler.class, "collectLatestMonthEnd");
  }

  @Test
  void companyFundFlowCollectsFridayOnSaturdayMorningForAllConditions() throws Exception {
    KofiaCompanyFundFlowService service = mock(KofiaCompanyFundFlowService.class);
    KofiaCompanyFundFlowRepository.JobView job = mock(KofiaCompanyFundFlowRepository.JobView.class);
    when(job.jobId()).thenReturn(UUID.randomUUID());
    when(service.startJob(
            KofiaCompanyFundFlowVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 2)))
        .thenReturn(job);

    new KofiaCompanyFundFlowScheduler(service, SATURDAY_CLOCK).collectPreviousBusinessDate();

    verify(service)
        .startJob(
            KofiaCompanyFundFlowVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 2));
    assertWeekdaySchedule(KofiaCompanyFundFlowScheduler.class, "collectPreviousBusinessDate");
  }

  private void assertWeekdaySchedule(Class<?> schedulerClass, String methodName) throws Exception {
    Scheduled scheduled = schedulerClass.getMethod(methodName).getAnnotation(Scheduled.class);
    assertThat(scheduled.cron()).endsWith("0 20 8 * * MON-SAT}");
    assertThat(scheduled.zone()).endsWith("Asia/Seoul}");
    var cron = org.springframework.scheduling.support.CronExpression.parse("0 20 8 * * MON-SAT");
    assertThat(cron.next(java.time.LocalDateTime.of(2026, 10, 2, 9, 0)))
        .isEqualTo(java.time.LocalDateTime.of(2026, 10, 3, 8, 20));
    assertThat(cron.next(java.time.LocalDateTime.of(2026, 10, 3, 9, 0)))
        .isEqualTo(java.time.LocalDateTime.of(2026, 10, 5, 8, 20));
  }

  @Test
  void companyFundFlowUsesFridayOnMondayRatherThanEmptySunday() {
    var service = mock(KofiaCompanyFundFlowService.class);
    var job = mock(KofiaCompanyFundFlowRepository.JobView.class);
    when(service.startJob(
            KofiaCompanyFundFlowVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 2)))
        .thenReturn(job);
    var monday = Clock.fixed(Instant.parse("2026-10-04T23:20:00Z"), ZoneId.of("Asia/Seoul"));
    new KofiaCompanyFundFlowScheduler(service, monday).collectPreviousBusinessDate();
    verify(service)
        .startJob(
            KofiaCompanyFundFlowVariant.Stage.ALL_ACTIVE,
            LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 2));
  }
}
