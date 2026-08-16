package com.nanum.investment.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BondYieldCollector {
  Yield collect(String bondCode, LocalDate date);

  record Yield(
      LocalDate baseDate,
      String bondCode,
      String bondName,
      String countryCode,
      Integer maturityMonths,
      BigDecimal yieldRate,
      String sourceCode) {}
}
