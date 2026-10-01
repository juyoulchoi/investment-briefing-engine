package com.nanum.investment.marketdata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;

public record KofiaFinalQuotedYield(
    LocalDate baseDate,
    String instrumentName,
    String remainingTermName,
    BigDecimal morningYieldRate,
    BigDecimal afternoonYieldRate,
    BigDecimal dayChangePoint,
    BigDecimal previousYieldRate,
    BigDecimal yearHighYieldRate,
    BigDecimal yearLowYieldRate) {

  public static KofiaFinalQuotedYield from(LocalDate baseDate, JsonNode row) {
    return new KofiaFinalQuotedYield(
        baseDate,
        row.path("TMPV1").asText(),
        row.path("TMPV2").asText(),
        decimal(row, "TMPV3"),
        decimal(row, "TMPV4"),
        decimal(row, "TMPV5"),
        decimal(row, "TMPV6"),
        decimal(row, "TMPV8"),
        decimal(row, "TMPV9"));
  }

  private static BigDecimal decimal(JsonNode row, String field) {
    JsonNode value = row.path(field);
    if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) return null;
    return new BigDecimal(value.asText().replace(",", ""));
  }
}
