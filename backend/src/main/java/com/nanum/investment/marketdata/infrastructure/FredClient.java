package com.nanum.investment.marketdata.infrastructure;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public interface FredClient {
  SeriesMetadata metadata(String seriesCode);

  List<Observation> observations(
      String seriesCode, LocalDate from, LocalDate to, String units, String aggregation);

  record SeriesMetadata(
      String seriesCode,
      String title,
      LocalDate observationStart,
      LocalDate observationEnd,
      String frequency,
      String frequencyShort,
      String units,
      String seasonalAdjustment,
      OffsetDateTime lastUpdated) {}

  record Observation(
      LocalDate observationDate,
      BigDecimal value,
      LocalDate realtimeStart,
      LocalDate realtimeEnd) {}
}
