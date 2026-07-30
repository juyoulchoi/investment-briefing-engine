package com.nanum.investment.calculation;
import java.math.BigDecimal; import java.util.List;
public record MarketScoreResult(BigDecimal totalScore,BigDecimal trendScore,BigDecimal flowScore,
 BigDecimal futuresProgramScore,BigDecimal breadthScore,BigDecimal volatilityScore,BigDecimal exchangeScore,
 BigDecimal rateScore,BigDecimal liquidityScore,BigDecimal confidenceRate,List<CalculationReason> reasons){}
