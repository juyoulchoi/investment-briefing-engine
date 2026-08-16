package com.nanum.investment.holding.domain;

import jakarta.validation.constraints.*;

public record StockPosition(
    @NotBlank String account,
    @NotBlank String code,
    @NotBlank String name,
    @PositiveOrZero long minimumBuyAmount,
    @PositiveOrZero long maximumBuyAmount,
    @DecimalMin("0.0") @DecimalMax("10.0") double maximumIncreaseMultiplier,
    double profitLossPct,
    double stockDrawdownPct,
    @DecimalMin("0.0") @DecimalMax("1.0") double targetWeight,
    @DecimalMin("0.0") @DecimalMax("1.0") double currentWeight,
    @DecimalMin("0.0") @DecimalMax("100.0") double fundamentalScore,
    @DecimalMin("0.0") @DecimalMax("100.0") double valuationScore,
    @DecimalMin("0.0") @DecimalMax("100.0") double themeRiskScore,
    boolean regularBuyEnabled) {
  public StockPosition(
      String account,
      String code,
      String name,
      long minimumBuyAmount,
      long maximumBuyAmount,
      double profitLossPct,
      double stockDrawdownPct,
      double targetWeight,
      double currentWeight,
      double fundamentalScore,
      double valuationScore,
      double themeRiskScore,
      boolean regularBuyEnabled) {
    this(
        account,
        code,
        name,
        minimumBuyAmount,
        maximumBuyAmount,
        3.0,
        profitLossPct,
        stockDrawdownPct,
        targetWeight,
        currentWeight,
        fundamentalScore,
        valuationScore,
        themeRiskScore,
        regularBuyEnabled);
  }

  public boolean isOverweight() {
    return targetWeight > 0 && currentWeight > targetWeight * 1.20;
  }

  public double effectiveMaximumIncreaseMultiplier() {
    return maximumIncreaseMultiplier > 0 ? maximumIncreaseMultiplier : 3.0;
  }

  public long effectiveMaximumBuyAmount() {
    return maximumBuyAmount > 0
        ? maximumBuyAmount
        : Math.round(minimumBuyAmount * effectiveMaximumIncreaseMultiplier());
  }
}
