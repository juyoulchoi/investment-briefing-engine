package com.nanum.investment.briefing.application;

import java.math.*;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class BriefingNumberFormatter {
  public String integer(Number value) { return grouped(value.longValue()); }
  public String score(Number value) { return integer(value) + "점"; }
  public String percent(Number value) { return integer(value) + "%"; }
  public String percentagePoint(Number value) {
    long n = value.longValue();
    return (n > 0 ? "+" : "") + grouped(n) + "%p";
  }
  public String decimal(Number value) {
    return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
  }
  public String exchangeRate(Number value) { return grouped(Math.round(value.doubleValue())) + "원"; }
  public String wonFlow(Number value) {
    BigDecimal eok = new BigDecimal(value.toString());
    if (eok.abs().compareTo(BigDecimal.valueOf(10_000)) >= 0)
      return decimal(eok.divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP)) + "조원";
    return grouped(eok.setScale(0, RoundingMode.HALF_UP).longValue()) + "억원";
  }
  private String grouped(long value) { return NumberFormat.getIntegerInstance(Locale.KOREA).format(value); }
}
