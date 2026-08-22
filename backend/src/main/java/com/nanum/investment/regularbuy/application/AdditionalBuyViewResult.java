package com.nanum.investment.regularbuy.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdditionalBuyViewResult(
    LocalDate baseDate,
    BigDecimal reserveAmount,
    BigDecimal recommendedTotal,
    BigDecimal usageRate,
    List<AccountSummary> accounts,
    List<Candidate> candidates) {
  public record AccountSummary(
      Long accountId,
      String accountType,
      BigDecimal reserveAmount,
      BigDecimal recommendedTotal,
      BigDecimal usageRate) {}

  public record Candidate(
      Long additionalBuyId,
      Long accountId,
      String accountType,
      Long stockId,
      String stockCode,
      String stockName,
      String eligibleYn,
      Integer priority,
      BigDecimal score,
      BigDecimal recommendedAmount,
      String reason,
      String executedYn) {}
}
