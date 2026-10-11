package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.nanum.investment.marketdata.infrastructure.ExchangeRateSource;
import com.nanum.investment.marketdata.infrastructure.YahooExchangeRateCollector;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class ExchangeRateSharedConsumerTest {
  @Test
  void consumerRefreshReadsSourceAndNeverCallsYahooOrWritesLocalDatabase() {
    var collector = mock(YahooExchangeRateCollector.class);
    var local = mock(JdbcClient.class);
    var source = mock(ExchangeRateSource.class);
    var statement = mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
    var query = mock(JdbcClient.ResultQuerySpec.class);
    when(source.shared()).thenReturn(true);
    when(source.sql(anyString())).thenReturn(statement);
    when(statement.query()).thenReturn(query);
    when(query.listOfRows()).thenReturn(List.of(Map.of("base_date", "2026-10-09")));
    var service = new ExchangeRateService(collector, local, source);
    assertThat(service.collect(LocalDate.of(2026,10,1), LocalDate.of(2026,10,9)).savedCount()).isZero();
    verifyNoInteractions(collector, local);
  }
}
