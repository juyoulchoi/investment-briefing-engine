package com.nanum.investment.marketdata.domain;

import java.time.*;
import java.util.List;

public record MarketDataValidationResult(
    LocalDate baseDate,
    OffsetDateTime validatedAt,
    boolean valid,
    DataStatus dataStatus,
    int confidence,
    List<ComponentResult> components,
    List<String> errors,
    List<String> warnings) {
  public record ComponentResult(
      String code,
      String name,
      boolean valid,
      DataStatus dataStatus,
      LocalDate latestDataDate,
      long recordCount,
      List<String> errors,
      List<String> warnings) {}
}
