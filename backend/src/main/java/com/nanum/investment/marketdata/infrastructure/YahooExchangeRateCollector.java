package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.common.infrastructure.external.ExternalApiRetryExecutor;
import com.nanum.investment.common.infrastructure.external.ExternalRestClientFactory;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class YahooExchangeRateCollector implements ExchangeRateCollector {
  private static final Map<String, String> SYMBOLS = Map.of("USD/KRW", "KRW=X");
  private final RestClient client;
  private final ExternalApiRetryExecutor retry;

  public YahooExchangeRateCollector(
      @Value("${exchange-rate.yahoo.base-url:${overseas.yahoo.base-url}}") String baseUrl,
      ExternalRestClientFactory clients,
      ExternalApiRetryExecutor retry) {
    this.client =
        clients
            .builder(baseUrl)
            .defaultHeader("User-Agent", "Mozilla/5.0 investment-briefing-engine/1.0")
            .defaultHeader("Accept", "application/json")
            .build();
    this.retry = retry;
  }

  @Override
  public Quote collect(String base, String quote, LocalDate date) {
    return collectRange(base, quote, date, date).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("해당 날짜의 환율이 없습니다."));
  }

  public List<Quote> collectRange(
      String requestedBase, String requestedQuote, LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("유효하지 않은 조회 기간입니다.");
    String base = currency(requestedBase),
        quote = currency(requestedQuote),
        symbol = SYMBOLS.get(base + "/" + quote);
    if (symbol == null) throw new IllegalArgumentException("지원하지 않는 통화쌍입니다: " + base + "/" + quote);
    long period1 = from.atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
        period2 = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    JsonNode response =
        retry.execute(
            () ->
                client
                    .get()
                    .uri(
                        u ->
                            u.pathSegment(symbol)
                                .queryParam("period1", period1)
                                .queryParam("period2", period2)
                                .queryParam("interval", "1d")
                                .queryParam("events", "div,splits")
                                .build())
                    .retrieve()
                    .body(JsonNode.class));
    JsonNode chart = response == null ? null : response.path("chart");
    if (chart == null || !chart.path("error").isNull() || chart.path("result").isEmpty())
      throw new IllegalStateException(
          "Yahoo Finance 환율을 받지 못했습니다: " + (chart == null ? "빈 응답" : chart.path("error")));
    JsonNode result = chart.path("result").get(0),
        timestamps = result.path("timestamp"),
        closes = result.path("indicators").path("quote").get(0).path("close");
    ZoneId zone = ZoneId.of(result.path("meta").path("exchangeTimezoneName").asText("UTC"));
    List<Quote> quotes = new ArrayList<>();
    for (int i = 0; i < timestamps.size(); i++) {
      JsonNode close = i < closes.size() ? closes.get(i) : null;
      if (close == null || !close.isNumber() || close.decimalValue().signum() <= 0) continue;
      LocalDate day = Instant.ofEpochSecond(timestamps.get(i).asLong()).atZone(zone).toLocalDate();
      if (!day.isBefore(from) && !day.isAfter(to))
        quotes.add(new Quote(day, base, quote, close.decimalValue(), "YAHOO"));
    }
    return quotes;
  }

  private String currency(String value) {
    String result = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    if (!result.matches("[A-Z]{3}"))
      throw new IllegalArgumentException("유효하지 않은 통화 코드입니다: " + value);
    return result;
  }
}
