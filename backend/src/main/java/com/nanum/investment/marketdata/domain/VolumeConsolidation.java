package com.nanum.investment.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Versioned, read-only screening results. A price/volume match is not a verified buy signal. */
public final class VolumeConsolidation {
  private VolumeConsolidation() {}

  public record Bar(
      LocalDate date,
      String market,
      String code,
      String name,
      String section,
      BigDecimal open,
      BigDecimal high,
      BigDecimal low,
      BigDecimal close,
      BigDecimal volume,
      BigDecimal value,
      BigDecimal listedShares) {}

  public record Master(LocalDate date, String kind, String securityGroup) {}

  public record Rules(
      String version,
      int observationDays,
      int baselineDays,
      BigDecimal minReturnPct,
      BigDecimal maxReturnPct,
      BigDecimal maxRangePct,
      BigDecimal minVolumeMultiple,
      BigDecimal spikeMultiple,
      int minSpikeDays,
      BigDecimal sustainedMultiple,
      int minSustainedDays,
      BigDecimal minAverageValue,
      BigDecimal breakoutVolumeMultiple,
      int breakoutDeadlineDays,
      int retestDeadlineDays,
      BigDecimal retestTolerancePct) {
    public static Rules defaults() {
      return new Rules(
          "VOLUME_CONSOLIDATION_V1",
          15,
          60,
          new BigDecimal("-5"),
          new BigDecimal("5"),
          new BigDecimal("10"),
          new BigDecimal("2"),
          new BigDecimal("5"),
          1,
          new BigDecimal("2"),
          3,
          new BigDecimal("2000000000"),
          new BigDecimal("2"),
          10,
          5,
          new BigDecimal("1"));
    }
  }

  public record Metrics(
      BigDecimal returnPct,
      BigDecimal closeRangePct,
      BigDecimal volumeMultiple,
      BigDecimal volumeIncreasePct,
      int spikeDays,
      int sustainedDays,
      BigDecimal maxDailyVolumeMultiple,
      BigDecimal averageValue,
      BigDecimal boxHigh,
      BigDecimal boxLow,
      BigDecimal close,
      BigDecimal movingAverage20,
      boolean aboveMovingAverage20,
      BigDecimal return60Pct,
      BigDecimal drawdown60Pct) {}

  public record Candidate(
      String market,
      String stockCode,
      String stockName,
      LocalDate baseDate,
      LocalDate masterDate,
      boolean quantitativeMatch,
      String verificationStatus,
      List<String> exclusionReasons,
      List<String> checksRequired,
      Metrics metrics,
      BigDecimal foreignNetAmount,
      BigDecimal institutionNetAmount,
      String flowStatus,
      String disclosureStatus) {}

  public record Coverage(
      String market, LocalDate latestDate, LocalDate masterDate, int latestStockCount) {}

  public record Screen(
      LocalDate requestedDate,
      LocalDate baseDate,
      LocalDate observationFrom,
      LocalDate baselineFrom,
      LocalDate baselineTo,
      int availableDays,
      String dateCoverageStatus,
      List<LocalDate> unverifiedWeekdays,
      Rules rules,
      List<Coverage> coverage,
      int universeCount,
      int matchCount,
      Map<String, Long> exclusionCounts,
      List<String> warnings,
      List<Candidate> rows) {}

  public record Event(LocalDate date, String type, BigDecimal close, BigDecimal volumeMultiple) {}

  public record Tracking(
      String stockCode,
      LocalDate selectionDate,
      LocalDate evaluatedThrough,
      BigDecimal boxHigh,
      BigDecimal boxLow,
      String state,
      List<Event> events,
      List<String> warnings) {}
}
