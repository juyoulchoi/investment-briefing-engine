package com.nanum.investment.holding.api.response;

import com.nanum.investment.holding.domain.HoldingStatus;
import com.nanum.investment.holding.domain.WeightStatus;
import java.math.BigDecimal;

public record HoldingApiResponse(
    Long holdingId,
    Long accountId,
    String accountName,
    Long stockId,
    String stockCode,
    String stockName,
    BigDecimal holdingQuantity,
    BigDecimal averagePrice,
    BigDecimal wholeSharePurchaseAmount,
    BigDecimal fractionalSharePurchaseAmount,
    BigDecimal currentPrice,
    BigDecimal evaluationAmount,
    BigDecimal profitLossRate,
    BigDecimal targetWeight,
    BigDecimal currentWeight,
    BigDecimal investmentAssetWeight,
    WeightStatus weightStatus,
    String weightStatusName,
    HoldingStatus holdingStatus) {}
