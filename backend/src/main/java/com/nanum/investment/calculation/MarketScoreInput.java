package com.nanum.investment.calculation;

import java.math.BigDecimal;

public record MarketScoreInput(BigDecimal mainIndexChangeRate, BigDecimal subIndexChangeRate,
        BigDecimal foreignNetAmount,
        BigDecimal institutionNetAmount, BigDecimal foreignFuturesNetQuantity, BigDecimal programNetAmount,
        BigDecimal exchangeChangeRate, BigDecimal volatilityIndexValue, BigDecimal volatilityIndexChangeRate,
        BigDecimal marketBreadthRate, BigDecimal newHighLowRatio, BigDecimal liquidityScore,
        BigDecimal usTwoYearYield, BigDecimal usTenYearYield, BigDecimal dataConfidenceRate) {
}
