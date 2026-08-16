package com.nanum.investment.request;

import com.nanum.investment.domain.MarketPhase;
import com.nanum.investment.domain.WeightStatus;
import java.math.BigDecimal;

public record RiskInput(
    boolean individualStock,
    boolean highRiskProduct,
    BigDecimal stockReturnRate,
    WeightStatus weightStatus,
    MarketPhase marketPhase,
    boolean accumulationPaused) {
  public static RiskInput from(
      StockAnalysisInput input, WeightStatus weightStatus, MarketPhase marketPhase) {
    boolean individualStock = input.benchmarkCode() == null || input.benchmarkCode().isBlank();
    boolean highRiskProduct = "THEME".equalsIgnoreCase(input.stockGrade());
    return new RiskInput(
        individualStock,
        highRiskProduct,
        input.stockReturnRate(),
        weightStatus,
        marketPhase,
        input.accumulationPaused());
  }
}
