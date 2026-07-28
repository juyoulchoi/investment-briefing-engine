package com.nanum.investment.domain;

import jakarta.validation.constraints.*;

public record StockPosition(
        @NotBlank String account, @NotBlank String code, @NotBlank String name,
        @PositiveOrZero long minimumBuyAmount, @PositiveOrZero long maximumBuyAmount,
        double profitLossPct, double stockDrawdownPct,
        @DecimalMin("0.0") @DecimalMax("1.0") double targetWeight,
        @DecimalMin("0.0") @DecimalMax("1.0") double currentWeight,
        @DecimalMin("0.0") @DecimalMax("100.0") double fundamentalScore,
        @DecimalMin("0.0") @DecimalMax("100.0") double valuationScore,
        @DecimalMin("0.0") @DecimalMax("100.0") double themeRiskScore,
        boolean regularBuyEnabled) {
    public boolean isOverweight() {
        return targetWeight > 0 && currentWeight > targetWeight * 1.20;
    }

    public long effectiveMaximumBuyAmount(double m) {
        return maximumBuyAmount > 0 ? maximumBuyAmount : Math.round(minimumBuyAmount * m);
    }
}
