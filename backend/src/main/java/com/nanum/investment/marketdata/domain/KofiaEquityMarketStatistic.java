package com.nanum.investment.marketdata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Map;

public record KofiaEquityMarketStatistic(
    String marketCode,
    BigDecimal indexValue,
    BigDecimal tradingQuantity,
    BigDecimal tradingAmount,
    BigDecimal marketCapitalization,
    BigDecimal foreignMarketCapitalization,
    BigDecimal foreignMarketCapitalizationRate,
    BigDecimal quantityUnitMultiplier,
    BigDecimal amountUnitMultiplier) {

  public static KofiaEquityMarketStatistic from(
      KofiaDataset dataset, JsonNode row, Map<String, Object> requestParameters) {
    String marketCode =
        switch (dataset) {
          case KOSPI_MARKET -> "KOSPI";
          case KOSDAQ_MARKET -> "KOSDAQ";
          default -> throw new IllegalArgumentException("주식시장 통계 Dataset이 아닙니다: " + dataset);
        };
    return new KofiaEquityMarketStatistic(
        marketCode,
        decimal(row.path("TMPV2")),
        decimal(row.path("TMPV3")),
        decimal(row.path("TMPV4")),
        decimal(row.path("TMPV5")),
        decimal(row.path("TMPV6")),
        decimal(row.path("TMPV7")),
        decimal(requestParameters.get("tmpV41")),
        decimal(requestParameters.get("tmpV40")));
  }

  private static BigDecimal decimal(Object value) {
    if (value instanceof JsonNode node) {
      if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) return null;
      return new BigDecimal(node.asText().replace(",", ""));
    }
    if (value == null || value.toString().isBlank() || "null".equals(value.toString())) return null;
    return new BigDecimal(value.toString().replace(",", ""));
  }
}
