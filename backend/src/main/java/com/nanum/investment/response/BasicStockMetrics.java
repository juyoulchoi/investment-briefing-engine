package com.nanum.investment.response;

import java.math.BigDecimal;

public record BasicStockMetrics(
    BigDecimal purchaseAmount,
    BigDecimal marketValue,
    BigDecimal profitAmount,
    BigDecimal stockReturnRate,
    BigDecimal currentWeight,
    BigDecimal weightDifference) {}
