package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.marketdata.infrastructure.VolumeConsolidationRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class VolumeConsolidationServiceTest {
  private final VolumeConsolidationRepository repository =
      mock(VolumeConsolidationRepository.class);
  private final VolumeConsolidationService service = new VolumeConsolidationService(repository);

  @Test
  void noDataIsExplicitAndDoesNotAttemptToReadBars() {
    LocalDate day = LocalDate.of(2025, 1, 1);
    when(repository.coverage(day)).thenReturn(List.of());
    when(repository.dates(day, 75)).thenReturn(List.of());
    var screen = service.screen(day);
    assertThat(screen.baseDate()).isNull();
    assertThat(screen.availableDays()).isZero();
    assertThat(screen.warnings()).contains("75거래일 데이터가 부족합니다.");
    verify(repository, never()).bars(any(), any(), any(), any());
  }

  @Test
  void rejectsTodayAndFutureDatesWithoutDatabaseAccess() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    assertThatThrownBy(() -> service.screen(today)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.screen(today.plusDays(1)))
        .isInstanceOf(BusinessException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void defaultsToYesterdayInSeoul() {
    LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
    when(repository.coverage(yesterday)).thenReturn(List.of());
    when(repository.dates(yesterday, 75)).thenReturn(List.of());
    assertThat(service.screen(null).requestedDate()).isEqualTo(yesterday);
  }

  @Test
  void rejectsInvalidTrackingInputs() {
    assertThatThrownBy(
            () -> service.track("US", "005930", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                service.track(
                    "KOSPI", "005930", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 2, 1)))
        .isInstanceOf(BusinessException.class);
    verifyNoInteractions(repository);
  }

  @Test
  void refusesTrackingAcrossUnexplainedWeekdayGaps() {
    LocalDate end = LocalDate.of(2025, 5, 1);
    LocalDate start = end.minusDays(100);
    when(repository.dates(end, 75)).thenReturn(List.of(start, end));
    when(repository.unverifiedWeekdays(start, end)).thenReturn(List.of(end.minusDays(1)));
    assertThatThrownBy(() -> service.track("KOSPI", "005930", end, end))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("미확인인 평일");
    verify(repository, never()).bars(any(), any(), any(), any());
  }
}
