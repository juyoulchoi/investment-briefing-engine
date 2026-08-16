package com.nanum.investment.marketdata.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MarketSentimentGenerationResult(LocalDate baseDate, List<Sentiment> sentiments) {
  public record Sentiment(
      Long sentimentId,
      String snapshotCode,
      BigDecimal score,
      SentimentPhase phase,
      BigDecimal confidenceRate,
      boolean structuralDamage,
      DataStatus dataStatus,
      String keyReason) {}
}
