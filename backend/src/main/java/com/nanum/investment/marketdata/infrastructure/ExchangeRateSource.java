package com.nanum.investment.marketdata.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

/** Dedicated read connection; never falls back to a stale consumer database on failure. */
@Component
public class ExchangeRateSource {
  private final JdbcClient jdbc;
  private final boolean shared;

  public ExchangeRateSource(JdbcClient local, Environment env) {
    shared = env.getProperty("EXCHANGE_SOURCE_ENABLED", Boolean.class, false);
    if (shared) {
      var source = new DriverManagerDataSource();
      source.setUrl(env.getRequiredProperty("EXCHANGE_SOURCE_URL"));
      source.setUsername(env.getRequiredProperty("EXCHANGE_SOURCE_USERNAME"));
      source.setPassword(env.getRequiredProperty("EXCHANGE_SOURCE_PASSWORD"));
      jdbc = JdbcClient.create(source);
    } else {
      jdbc = local;
    }
  }

  public boolean shared() { return shared; }
  public JdbcClient.StatementSpec sql(String sql) { return jdbc.sql(sql); }
  public BigDecimal latestRate() {
    return sql("SELECT \"EXCH_RT\" FROM \"TB_EXCH_DAY\" WHERE \"BASE_CURR_CD\"='USD' AND \"QUOTE_CURR_CD\"='KRW' ORDER BY \"BASE_DT\" DESC LIMIT 1")
        .query(BigDecimal.class).optional().orElseThrow(() -> new IllegalStateException("USD/KRW source data is unavailable"));
  }
  public LocalDate latestDate(LocalDate day) {
    return sql("SELECT max(\"BASE_DT\") FROM \"TB_EXCH_DAY\" WHERE \"BASE_DT\"<=:day")
        .param("day", day).query(LocalDate.class).optional().orElse(null);
  }
}
