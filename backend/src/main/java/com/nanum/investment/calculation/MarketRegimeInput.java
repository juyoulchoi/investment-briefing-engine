package com.nanum.investment.calculation;

import java.math.BigDecimal;

public record MarketRegimeInput(BigDecimal marketScore, BigDecimal mainIndexChangeRate, BigDecimal marketBreadthRate,
        BigDecimal volatilityIndexValue, BigDecimal volatilityIndexChangeRate, BigDecimal exchangeChangeRate,
        BigDecimal foreignNetAmount, BigDecimal foreignFuturesNetQuantity, BigDecimal structuralDamageRate,
        boolean emergencyEvent) {
}
