package com.nanum.investment.common.api.response;

import com.nanum.investment.common.domain.AssetType;
import com.nanum.investment.common.domain.StockGrade;

public record StockApiResponse(
    Long stockId,
    String stockCode,
    String stockName,
    String marketCode,
    String countryCode,
    String currencyCode,
    AssetType assetType,
    StockGrade stockGrade,
    String sectorName,
    String useYn) {}
