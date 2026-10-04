package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.marketdata.domain.VolumeConsolidation.*;
import com.nanum.investment.marketdata.infrastructure.VolumeConsolidationRunRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VolumeConsolidationRunServiceTest {
  private final VolumeConsolidationService screens = mock(VolumeConsolidationService.class);
  private final VolumeConsolidationRunRepository runs =
      mock(VolumeConsolidationRunRepository.class);
  private final VolumeConsolidationRunService service =
      new VolumeConsolidationRunService(screens, runs, new ObjectMapper().findAndRegisterModules());
  private final LocalDate date = LocalDate.of(2025, 3, 3);

  private Screen result(String status) {
    return new Screen(
        date,
        date,
        date.minusDays(20),
        date.minusDays(100),
        date.minusDays(21),
        75,
        status,
        List.of(),
        Rules.defaults(),
        List.of(new Coverage("KOSPI", date, date, 1)),
        1,
        0,
        Map.of(),
        List.of(),
        List.of());
  }

  @Test
  void persistsProvisionalResultAfterSuccessfulCollectionWithoutTreatingItAsVerified() {
    when(screens.screen(date)).thenReturn(result("DATE_GAPS_UNVERIFIED"));
    when(runs.find(any()))
        .thenReturn(
            Optional.of(mock(com.nanum.investment.marketdata.domain.VolumeConsolidationRun.class)));
    service.afterCollection(date);
    var order = inOrder(runs, screens);
    order
        .verify(runs)
        .start(any(), eq("AFTER_KRX_COLLECTION"), eq(date), eq("VOLUME_CONSOLIDATION_V1"));
    order.verify(screens).screen(date);
    order
        .verify(runs)
        .finish(any(), eq("PARTIAL"), eq(date), eq(0), contains("DATE_GAPS_UNVERIFIED"), isNull());
  }

  @Test
  void persistsCalculatedResultWhenCoverageHasNoGaps() {
    when(screens.screen(date)).thenReturn(result("NO_UNEXPLAINED_GAPS"));
    when(runs.find(any()))
        .thenReturn(
            Optional.of(mock(com.nanum.investment.marketdata.domain.VolumeConsolidationRun.class)));
    service.afterCollection(date);
    verify(runs).finish(any(), eq("CALCULATED"), eq(date), eq(0), anyString(), isNull());
  }

  @Test
  void skipsUncollectedAndUnfinishedDailyPrices() {
    when(runs.find(any()))
        .thenReturn(
            Optional.of(mock(com.nanum.investment.marketdata.domain.VolumeConsolidationRun.class)));
    service.afterCollection(null);
    service.afterCollection(LocalDate.now(ZoneId.of("Asia/Seoul")));
    verifyNoInteractions(screens);
    verify(runs, times(2))
        .finish(any(), eq("SKIPPED"), nullable(LocalDate.class), isNull(), isNull(), anyString());
  }

  @Test
  void preservesFailureRecordAndDoesNotAbortPriceRefresh() {
    when(screens.screen(date)).thenThrow(new IllegalStateException("failed"));
    when(runs.find(any()))
        .thenReturn(
            Optional.of(mock(com.nanum.investment.marketdata.domain.VolumeConsolidationRun.class)));
    assertThatCode(() -> service.afterCollection(date)).doesNotThrowAnyException();
    verify(runs)
        .finish(
            any(), eq("FAILED"), eq(date), isNull(), isNull(), contains("IllegalStateException"));
  }

  @Test
  void auditDatabaseFailureDoesNotAbortBriefing() {
    doThrow(new IllegalStateException("db failure")).when(runs).start(any(), any(), any(), any());
    assertThatCode(() -> service.afterCollection(date)).doesNotThrowAnyException();
    verifyNoInteractions(screens);
  }
}
