package com.nanum.investment.marketdata.application;

import com.nanum.investment.briefing.application.MarketCalendarService;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KrxCollectionOutcomeClassifier {
  private final MarketCalendarService calendar;
  private final ZoneId zone;
  private final LocalTime publicationTime;

  public KrxCollectionOutcomeClassifier(MarketCalendarService calendar,
      @Value("${krx.eod-publication-zone:Asia/Seoul}") String zone,
      @Value("${krx.eod-publication-time:18:00}") LocalTime publicationTime) {
    this.calendar = calendar;
    this.zone = ZoneId.of(zone);
    this.publicationTime = publicationTime;
  }

  public String beforeRequest(LocalDate baseDate) {
    if (!calendar.isMarketOpen(baseDate, "KRX")) return "MARKET_HOLIDAY";
    ZonedDateTime now = ZonedDateTime.now(zone);
    if (baseDate.isAfter(now.toLocalDate())
        || (baseDate.equals(now.toLocalDate()) && now.toLocalTime().isBefore(publicationTime)))
      return "NOT_PUBLISHED_YET";
    return null;
  }

  public String afterSuccess(int receivedCount) {
    return receivedCount > 0 ? "DATA_RECEIVED" : "NO_DATA_UNEXPECTED";
  }
}
