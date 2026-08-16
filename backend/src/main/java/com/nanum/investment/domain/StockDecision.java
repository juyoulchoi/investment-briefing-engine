package com.nanum.investment.domain;

import java.util.List;

public record StockDecision(
    String account,
    String code,
    String name,
    ActionSignal action,
    double multiplier,
    long minimumBuyAmount,
    long maximumBuyAmount,
    double maximumIncreaseMultiplier,
    long recommendedBuyAmount,
    long reservedCash,
    String adjustmentReason,
    String cashPlan,
    List<String> reasons) {}
