package com.nanum.investment.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MarketSnapshotGenerationResult(
    LocalDate baseDate,
    DataStatus dataStatus,
    int validationConfidence,
    List<Snapshot> snapshots,
    List<String> validationWarnings) {
  public record Snapshot(
      Long snapshotId,
      String snapshotCode,
      String marketName,
      LocalDate sourceDate,
      BigDecimal mainIndexValue,
      BigDecimal mainIndexChangeRate,
      BigDecimal exchangeRate,
      BigDecimal volatilityIndexValue,
      Integer advancingStockCount,
      Integer decliningStockCount,
      Integer unchangedStockCount,
      BigDecimal marketBreadthRate) {}
}
