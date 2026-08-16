package com.nanum.investment.request;

import java.math.BigDecimal;

public record StockAnalysisInput(
    String accountType,
    String stockCode,
    String stockName,
    BigDecimal quantity,
    BigDecimal avgPrice,
    BigDecimal currentPrice,
    BigDecimal targetWeight,
    BigDecimal accountTotalAmount,
    BigDecimal availableCash,
    BigDecimal stockReturnRate,
    BigDecimal marketReturnRate,
    String stockGrade,
    String benchmarkCode,
    String accumulationCycle,
    String accumulationWeekDays,
    Integer accumulationMonthDay,
    boolean accumulationPaused,
    BigDecimal currentMarketIndex,
    BigDecimal recentPeakIndex,
    boolean individualStock,
    boolean leveragedProduct,
    boolean thematicEtf,
    String marketCode,
    boolean fundamentalDamaged,
    boolean aboveMa5,
    boolean aboveMa20,
    boolean benchmarkStable,
    boolean existingRiskResolved,
    boolean rebuyEligible) {}
