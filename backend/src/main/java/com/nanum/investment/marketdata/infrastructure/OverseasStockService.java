package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.common.infrastructure.external.ExternalApiRetryExecutor;
import com.nanum.investment.common.infrastructure.external.ExternalRestClientFactory;
import com.nanum.investment.holding.application.HoldingPriceSyncService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class OverseasStockService {
  private final JdbcClient jdbc;
  private final RestClient client;
  private final HoldingPriceSyncService holdingPriceSync;
  private final ExternalApiRetryExecutor retry;

  public OverseasStockService(
      JdbcClient jdbc,
      @Value("${overseas.yahoo.base-url}") String baseUrl,
      HoldingPriceSyncService holdingPriceSync,
      ExternalRestClientFactory clients,
      ExternalApiRetryExecutor retry) {
    this.jdbc = jdbc;
    this.client =
        clients
            .builder(baseUrl)
            .defaultHeader("User-Agent", "Mozilla/5.0 investment-briefing-engine/1.0")
            .defaultHeader("Accept", "application/json")
            .build();
    this.holdingPriceSync = holdingPriceSync;
    this.retry = retry;
  }

  @Transactional
  public Map<String, Object> refresh(String requestedSymbol) {
    String symbol = normalize(requestedSymbol);
    JsonNode result =
        fetch(
            symbol,
            uri ->
                uri.pathSegment(yahooSymbol(symbol))
                    .queryParam("range", "5d")
                    .queryParam("interval", "1d")
                    .queryParam("events", "div,splits")
                    .build());
    saveMaster(symbol, result.path("meta"));
    saveDailyPrices(symbol, result, LocalDate.MIN, LocalDate.MAX);
    holdingPriceSync.refreshStock("US", symbol);
    return find(symbol);
  }

  @Transactional
  public HistoryCollectionResult collectHistory(
      String requestedSymbol, LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("유효하지 않은 조회 기간입니다.");
    String symbol = normalize(requestedSymbol);
    long period1 = from.atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
    long period2 = to.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toEpochSecond();
    JsonNode result =
        fetch(
            symbol,
            uri ->
                uri.pathSegment(yahooSymbol(symbol))
                    .queryParam("period1", period1)
                    .queryParam("period2", period2)
                    .queryParam("interval", "1d")
                    .queryParam("events", "div,splits")
                    .build());
    saveMaster(symbol, result.path("meta"));
    int saved = saveDailyPrices(symbol, result, from, to);
    holdingPriceSync.refreshStock("US", symbol);
    return new HistoryCollectionResult(symbol, from, to, saved);
  }

  public List<Map<String, Object>> findHistory(
      String requestedSymbol, LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            SELECT symbol,trading_day,open_price,high_price,low_price,close_price,adjusted_close,
              volume,provider,updated_at FROM tb_prc_day
            WHERE symbol=:symbol AND trading_day BETWEEN :from AND :to ORDER BY trading_day
            """)
        .param("symbol", normalize(requestedSymbol))
        .param("from", from)
        .param("to", to)
        .query()
        .listOfRows();
  }

  public List<Map<String, Object>> findAll() {
    return jdbc.sql(
            """
            SELECT s.stock_code AS symbol,s.stock_name AS company_name,s.exchange_name,s.currency,
              latest.trading_day,latest.close_price AS price,previous.close_price AS previous_close,
              latest.close_price-previous.close_price AS change_amount,
              CASE WHEN previous.close_price IS NULL OR previous.close_price=0 THEN NULL
                ELSE round((latest.close_price-previous.close_price)*100/previous.close_price,4)::text||'%' END AS change_percent,
              latest.volume,s.provider,latest.updated_at,'OVERSEAS' AS account_type,
              'OVERSEAS' AS market_scope,NULL::varchar AS stock_grade
            FROM tb_hold s
            JOIN LATERAL (
              SELECT * FROM tb_prc_day p
              WHERE p.symbol=s.stock_code ORDER BY trading_day DESC LIMIT 1
            ) latest ON true
            LEFT JOIN LATERAL (
              SELECT * FROM tb_prc_day p
              WHERE p.symbol=s.stock_code ORDER BY trading_day DESC OFFSET 1 LIMIT 1
            ) previous ON true
            WHERE s.listing_scope='OVERSEAS' AND s.active_yn='Y'
            ORDER BY s.stock_code
            """)
        .query()
        .listOfRows();
  }

  public Map<String, Object> find(String requestedSymbol) {
    var rows =
        jdbc.sql(
                """
            WITH prices AS (
              SELECT *, row_number() OVER (ORDER BY trading_day DESC) AS rn
              FROM tb_prc_day WHERE symbol=:symbol
            )
            SELECT s.stock_code AS symbol,s.stock_name AS company_name,s.exchange_name,s.currency,
              latest.trading_day,latest.open_price,latest.high_price,latest.low_price,
              latest.close_price AS price,previous.close_price AS previous_close,
              latest.close_price-previous.close_price AS change_amount,
              CASE WHEN previous.close_price IS NULL OR previous.close_price=0 THEN NULL
                ELSE round((latest.close_price-previous.close_price)*100/previous.close_price,4)::text||'%' END AS change_percent,
              latest.volume,s.provider,latest.updated_at,'OVERSEAS' AS account_type,
              'OVERSEAS' AS market_scope,NULL::varchar AS stock_grade
            FROM tb_hold s
            JOIN prices latest ON latest.rn=1
            LEFT JOIN prices previous ON previous.rn=2
            WHERE s.stock_code=:symbol AND s.listing_scope='OVERSEAS'
            """)
            .param("symbol", normalize(requestedSymbol))
            .query()
            .listOfRows();
    if (rows.isEmpty()) throw new IllegalArgumentException("저장된 해외주식 시세가 없습니다.");
    return rows.getFirst();
  }

  private JsonNode fetch(
      String symbol,
      java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uri) {
    JsonNode response = retry.execute(() -> client.get().uri(uri).retrieve().body(JsonNode.class));
    JsonNode chart = response == null ? null : response.path("chart");
    if (chart == null || !chart.path("error").isNull() || chart.path("result").isEmpty())
      throw new IllegalStateException(
          "Yahoo Finance 시세를 받지 못했습니다: " + (chart == null ? "빈 응답" : chart.path("error")));
    return chart.path("result").get(0);
  }

  private void saveMaster(String symbol, JsonNode meta) {
    jdbc.sql(
            """
            INSERT INTO "TB_STK"
              ("MKT_CD","STK_CD","STK_NM","STK_NM_EN","LIST_SCOPE","ASSET_TP","AST_TP","CNTRY_CD","CURR","CURR_CD","STK_GRADE","EXCH_NM","PRVDR")
            VALUES ('US',:symbol,:name,:name,'OVERSEAS','STOCK','STOCK','US',:currency,:currency,'CORE',:exchange,'YAHOO_FINANCE')
            ON CONFLICT("MKT_CD","STK_CD") DO UPDATE SET "STK_NM_EN"=EXCLUDED."STK_NM_EN",
              "EXCH_NM"=EXCLUDED."EXCH_NM","CURR"=EXCLUDED."CURR",
              "PRVDR"=EXCLUDED."PRVDR","MOD_DT"=CURRENT_TIMESTAMP,"UPD_DTTM"=CURRENT_TIMESTAMP
            """)
        .param("symbol", symbol)
        .param("name", meta.path("longName").asText(meta.path("shortName").asText(symbol)))
        .param("exchange", meta.path("fullExchangeName").asText(meta.path("exchangeName").asText()))
        .param("currency", meta.path("currency").asText())
        .update();
  }

  private int saveDailyPrices(String symbol, JsonNode result, LocalDate from, LocalDate to) {
    JsonNode meta = result.path("meta");
    JsonNode timestamps = result.path("timestamp");
    JsonNode quote = result.path("indicators").path("quote").get(0);
    JsonNode adjusted = result.path("indicators").path("adjclose");
    JsonNode adjustedValues =
        adjusted.isArray() && !adjusted.isEmpty() ? adjusted.get(0).path("adjclose") : null;
    ZoneId zone = ZoneId.of(meta.path("exchangeTimezoneName").asText("UTC"));
    int saved = 0;
    for (int i = 0; i < timestamps.size(); i++) {
      JsonNode close = arrayValue(quote.path("close"), i);
      if (close == null || close.isNull() || !close.isNumber()) continue;
      LocalDate day = Instant.ofEpochSecond(timestamps.get(i).asLong()).atZone(zone).toLocalDate();
      if (day.isBefore(from) || day.isAfter(to) || !isCompletedTradingDay(day, zone)) continue;
      jdbc.sql(
              """
                INSERT INTO "TB_PRC_DAY"
                  ("STK_ID","MKT_CD","STK_CD","TRADE_DT","OPEN_PRC","HIGH_PRC","LOW_PRC",
                   "CLS_PRC","ADJ_CLS_PRC","VOL","PRVDR","DATA_SRC_CD")
                VALUES ((SELECT "STK_ID" FROM "TB_STK" WHERE "MKT_CD"='US' AND "STK_CD"=:symbol),'US',:symbol,:day,:open,:high,:low,:close,:adjusted,:volume,'YAHOO_FINANCE','YAHOO')
                ON CONFLICT("MKT_CD","STK_CD","TRADE_DT") DO UPDATE SET
                  "OPEN_PRC"=EXCLUDED."OPEN_PRC","HIGH_PRC"=EXCLUDED."HIGH_PRC",
                  "LOW_PRC"=EXCLUDED."LOW_PRC","CLS_PRC"=EXCLUDED."CLS_PRC",
                  "ADJ_CLS_PRC"=EXCLUDED."ADJ_CLS_PRC","VOL"=EXCLUDED."VOL",
                  "PRVDR"=EXCLUDED."PRVDR","MOD_DT"=CURRENT_TIMESTAMP
                """)
          .param("symbol", symbol)
          .param("day", day)
          .param("open", decimal(arrayValue(quote.path("open"), i)))
          .param("high", decimal(arrayValue(quote.path("high"), i)))
          .param("low", decimal(arrayValue(quote.path("low"), i)))
          .param("close", decimal(close))
          .param("adjusted", decimal(arrayValue(adjustedValues, i)))
          .param("volume", longValue(quote.path("volume"), i))
          .update();
      saved++;
    }
    return saved;
  }

  private boolean isCompletedTradingDay(LocalDate day, ZoneId exchangeZone) {
    ZonedDateTime now = ZonedDateTime.now(exchangeZone);
    return day.isBefore(now.toLocalDate())
        || day.equals(now.toLocalDate()) && !now.toLocalTime().isBefore(LocalTime.of(16, 15));
  }

  private JsonNode arrayValue(JsonNode array, int index) {
    return array == null || !array.isArray() || index >= array.size() ? null : array.get(index);
  }

  private long longValue(JsonNode array, int index) {
    JsonNode value = arrayValue(array, index);
    return value == null || value.isNull() ? 0L : value.asLong();
  }

  private String yahooSymbol(String symbol) {
    return symbol.replace('.', '-');
  }

  private String normalize(String symbol) {
    String value = symbol == null ? "" : symbol.trim().toUpperCase();
    if (!value.matches("[A-Z0-9.^=:-]{1,30}"))
      throw new IllegalArgumentException("유효하지 않은 종목 심볼입니다.");
    return value;
  }

  private BigDecimal decimal(JsonNode value) {
    return value == null || value.isNull() ? BigDecimal.ZERO : value.decimalValue();
  }

  public record HistoryCollectionResult(
      String symbol, LocalDate from, LocalDate to, int savedCount) {}
}
