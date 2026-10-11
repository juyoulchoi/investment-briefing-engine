package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.nanum.investment.marketdata.infrastructure.ExchangeRateSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.env.MockEnvironment;

class ExchangeRateSourceFailureTest {
  @Test
  void failedSharedConnectionNeverFallsBackToConsumerDatabase() {
    var local = mock(JdbcClient.class);
    var env = new MockEnvironment()
        .withProperty("EXCHANGE_SOURCE_ENABLED", "true")
        .withProperty("EXCHANGE_SOURCE_URL", "jdbc:postgresql://127.0.0.1:1/missing?connectTimeout=1")
        .withProperty("EXCHANGE_SOURCE_USERNAME", "reader")
        .withProperty("EXCHANGE_SOURCE_PASSWORD", "test");
    var source = new ExchangeRateSource(local, env);
    assertThatThrownBy(source::latestRate).isInstanceOf(org.springframework.dao.DataAccessException.class);
    verifyNoInteractions(local);
  }
}
