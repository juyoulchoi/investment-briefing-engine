package com.nanum.investment.briefing.domain.calculation;

import java.math.BigDecimal;

public record MarketSentimentInput(
    BigDecimal newsFearScore,
    BigDecimal aiFatigueScore,
    BigDecimal earningsConfidenceScore,
    BigDecimal liquidityScore,
    BigDecimal flowScore,
    BigDecimal exchangePressureScore,
    BigDecimal ratePressureScore,
    BigDecimal volatilityPressureScore,
    BigDecimal dataConfidenceRate) {}
