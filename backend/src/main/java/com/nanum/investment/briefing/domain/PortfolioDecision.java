package com.nanum.investment.briefing.domain;

import com.nanum.investment.marketdata.domain.MarketAssessment;
import java.time.LocalDate;
import java.util.List;

public record PortfolioDecision(
    LocalDate decisionDate,
    MarketAssessment market,
    long totalMinimumBuyAmount,
    long totalRecommendedBuyAmount,
    long newlyReservedCash,
    long availableAdditionalBuyCash,
    List<StockDecision> stockDecisions,
    List<String> weeklyActions,
    List<String> monthlyActions) {}
