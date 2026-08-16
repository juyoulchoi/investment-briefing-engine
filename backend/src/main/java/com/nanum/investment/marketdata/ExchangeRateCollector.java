package com.nanum.investment.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExchangeRateCollector {
  Quote collect(String baseCurrency, String quoteCurrency, LocalDate date);

  record Quote(
      LocalDate baseDate,
      String baseCurrency,
      String quoteCurrency,
      BigDecimal exchangeRate,
      String sourceCode) {}
}
