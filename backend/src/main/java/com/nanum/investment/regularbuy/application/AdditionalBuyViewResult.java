package com.nanum.investment.regularbuy.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdditionalBuyViewResult(
    LocalDate baseDate,
    BigDecimal reserveAmount,
    BigDecimal recommendedTotal,
    BigDecimal usageRate,
    List<Candidate> candidates) {
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
