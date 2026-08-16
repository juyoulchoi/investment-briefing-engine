package com.nanum.investment.briefing.api.response;

import com.nanum.investment.briefing.domain.FinalAction;
import com.nanum.investment.common.domain.RiskLevel;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.marketdata.domain.MarketPhase;
import com.nanum.investment.regularbuy.domain.RebuySignal;
import com.nanum.investment.regularbuy.domain.RegularBuySignal;
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
