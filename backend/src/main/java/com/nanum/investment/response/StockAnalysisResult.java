package com.nanum.investment.response;

import com.nanum.investment.domain.*;
import java.math.BigDecimal;

public record StockAnalysisResult(
    String stockCode,
    BigDecimal purchaseAmount,
    BigDecimal marketValue,
    BigDecimal profitAmount,
    BigDecimal stockReturnRate,
    BigDecimal currentWeight,
    BigDecimal targetWeight,
    BigDecimal maximumWeight,
    BigDecimal weightDifference,
    WeightStatus weightStatus,
    BigDecimal marketDrawdownRate,
    MarketPhase marketPhase,
    int riskScore,
    RiskLevel riskLevel,
    java.util.List<String> riskReasons,
    RegularBuySignal regularBuySignal,
    BigDecimal additionalBuyAmount,
    RebuySignal rebuySignal,
    FinalAction finalAction) {}
