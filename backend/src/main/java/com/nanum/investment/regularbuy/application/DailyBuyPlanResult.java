package com.nanum.investment.regularbuy.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyBuyPlanResult(
    LocalDate baseDate,
    Long investmentDecisionId,
    BigDecimal additionalBuyTotal,
    BigDecimal rebuyTotal,
    List<Plan> additionalBuys,
    List<Plan> rebuys) {
  public record Plan(
      Long stockDecisionId,
      Long accountId,
      Long stockId,
      String stockCode,
      String signal,
      boolean eligible,
      Integer priority,
      BigDecimal score,
      BigDecimal recommendedAmount,
      String reason) {}
}
