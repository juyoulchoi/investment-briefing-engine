package com.nanum.investment.marketdata.domain;

import jakarta.validation.constraints.*;

public record MarketSnapshot(
    double indexDrawdownPct,
    double volatilityIndex,
    double foreignNetBuy,
    double institutionNetBuy,
    double individualNetBuy,
    double programNetBuy,
    double usdKrwChangePct,
    double us10yChangeBp,
    double breadthPct,
    @Min(0) @Max(100) int newsFearScore,
    @Min(0) @Max(100) int aiCapexFatigueScore,
    @Min(0) @Max(100) int earningsConfidenceScore,
    @Min(0) @Max(100) int liquidityScore,
    @NotNull String dominantNarrative) {}
