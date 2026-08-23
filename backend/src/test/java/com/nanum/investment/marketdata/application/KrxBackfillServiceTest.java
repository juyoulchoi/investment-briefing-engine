package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.marketdata.infrastructure.KrxBackfillRepository;
import com.nanum.investment.marketdata.infrastructure.KrxCollectionJobRepository;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KrxBackfillServiceTest {
  @Test
  void createsTradingDaysAndSkipsWeekendAndCalendarHoliday() {
    KrxBackfillRepository repository = mock(KrxBackfillRepository.class);
    KrxCollectionJobRepository dailyJobs = mock(KrxCollectionJobRepository.class);
    KrxBackfillJobRunner runner = mock(KrxBackfillJobRunner.class);
    KrxBackfillService service = new KrxBackfillService(repository, dailyJobs, runner);
    LocalDate monday =
        LocalDate.now()
            .minusDays(7)
            .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    LocalDate friday = monday.minusDays(3);
    when(repository.marketOpen(friday)).thenReturn(Optional.of(false));
    when(repository.marketOpen(friday.plusDays(1))).thenReturn(Optional.empty());
    when(repository.marketOpen(friday.plusDays(2))).thenReturn(Optional.empty());
    when(repository.marketOpen(monday)).thenReturn(Optional.of(true));
    when(repository.find(any())).thenAnswer(invocation -> view(invocation.getArgument(0)));

    service.start(friday, monday, List.of("KOSPI_STOCK_DAILY"), 500);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<KrxBackfillRepository.NewDay>> days = ArgumentCaptor.forClass(List.class);
    verify(repository)
        .create(any(), eq(friday), eq(monday), eq("KOSPI_STOCK_DAILY"), eq(500L), days.capture());
    assertThat(days.getValue())
        .extracting(KrxBackfillRepository.NewDay::status)
        .containsExactly("SKIPPED", "SKIPPED", "SKIPPED", "PENDING");
    verify(runner).run(any());
  }

  @Test
  void rejectsOverlappingActiveBackfill() {
    KrxBackfillRepository repository = mock(KrxBackfillRepository.class);
    KrxBackfillService service =
        new KrxBackfillService(
            repository, mock(KrxCollectionJobRepository.class), mock(KrxBackfillJobRunner.class));
    LocalDate from = LocalDate.of(2026, 8, 3);
    LocalDate to = LocalDate.of(2026, 8, 7);
    when(repository.hasActiveOverlap(from, to, "KOSPI_STOCK_DAILY")).thenReturn(true);

    assertThatThrownBy(() -> service.start(from, to, List.of("KOSPI_STOCK_DAILY"), 250))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("겹치는 활성");
    verify(repository, never()).create(any(), any(), any(), any(), anyLong(), any());
  }

  @Test
  void rejectsFutureAndUnsupportedDataset() {
    KrxBackfillService service =
        new KrxBackfillService(
            mock(KrxBackfillRepository.class),
            mock(KrxCollectionJobRepository.class),
            mock(KrxBackfillJobRunner.class));

    assertThatThrownBy(
            () -> service.start(LocalDate.now(), LocalDate.now().plusDays(1), List.of(), 250))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                service.start(
                    LocalDate.now().minusDays(1), LocalDate.now(), List.of("UNKNOWN_DATASET"), 250))
        .isInstanceOf(BusinessException.class);
  }

  private KrxBackfillRepository.BackfillJobView view(UUID id) {
    return new KrxBackfillRepository.BackfillJobView(
        id,
        LocalDate.now().minusDays(10),
        LocalDate.now().minusDays(7),
        "QUEUED",
        List.of("KOSPI_STOCK_DAILY"),
        500,
        4,
        0,
        0,
        3,
        1,
        null,
        null,
        null,
        null,
        null,
        List.of());
  }
}
