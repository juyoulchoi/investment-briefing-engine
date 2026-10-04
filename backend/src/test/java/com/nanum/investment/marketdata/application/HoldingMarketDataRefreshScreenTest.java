package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanum.investment.marketdata.domain.KrxDataset;
import com.nanum.investment.marketdata.infrastructure.OverseasStockService;
import com.nanum.investment.marketdata.infrastructure.YahooIndexService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class HoldingMarketDataRefreshScreenTest {
  @Test
  void runsScreenAfterAllDomesticDatasetsAndBeforeOtherPipelineStages() {
    var krx = mock(KrxMarketDataService.class);
    var runs = mock(VolumeConsolidationRunService.class);
    var overseas = mock(OverseasStockService.class);
    var indices = mock(YahooIndexService.class);
    var jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
    when(jdbc.sql(anyString()).query(String.class).list()).thenReturn(List.of());
    when(krx.collect(any(), any()))
        .thenAnswer(
            invocation ->
                new KrxMarketDataService.CollectionResult(
                    ((KrxDataset) invocation.getArgument(0)).name(),
                    invocation.getArgument(1),
                    10,
                    10));
    var result = new HoldingMarketDataRefreshService(krx, overseas, indices, jdbc, runs).refresh();
    var order = inOrder(krx, runs, indices);
    for (var dataset :
        List.of(
            KrxDataset.KOSPI_STOCK_DAILY,
            KrxDataset.KOSDAQ_STOCK_DAILY,
            KrxDataset.ETF_DAILY,
            KrxDataset.KOSPI_INDEX_DAILY))
      order.verify(krx).collect(eq(dataset), any(LocalDate.class));
    order.verify(runs).afterCollection(result.krxBaseDate());
    order.verify(indices).refresh("SP500");
    assertThat(result.success()).isTrue();
  }

  @Test
  void failedDomesticCollectionReportsSkippedScreenEvenWhenOtherSourcesContinue() {
    var krx = mock(KrxMarketDataService.class);
    var runs = mock(VolumeConsolidationRunService.class);
    var jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
    when(jdbc.sql(anyString()).query(String.class).list()).thenReturn(List.of());
    when(krx.collect(any(), any())).thenThrow(new IllegalStateException("KRX_AUTH_KEY missing"));
    var result =
        new HoldingMarketDataRefreshService(
                krx, mock(OverseasStockService.class), mock(YahooIndexService.class), jdbc, runs)
            .refresh();
    assertThat(result.success()).isFalse();
    verify(runs).afterCollection(null);
  }
}
