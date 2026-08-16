package com.nanum.investment.response;

import com.nanum.investment.domain.*;
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
    BigDecimal currentPrice,
    BigDecimal evaluationAmount,
    BigDecimal profitLossRate,
    BigDecimal targetWeight,
    BigDecimal currentWeight,
    WeightStatus weightStatus,
    HoldingStatus holdingStatus) {}
