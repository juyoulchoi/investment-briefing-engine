package com.nanum.investment.marketdata;

import java.time.LocalDate;
import java.util.*;
import org.slf4j.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeRateService {
  private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
  private static final String BASE = "USD", QUOTE = "KRW";
  private final YahooExchangeRateCollector collector;
  private final JdbcClient jdbc;

  public ExchangeRateService(YahooExchangeRateCollector collector, JdbcClient jdbc) {
    this.collector = collector;
    this.jdbc = jdbc;
  }

  @Transactional
  public CollectionResult collect(LocalDate from, LocalDate to) {
    List<ExchangeRateCollector.Quote> values = collector.collectRange(BASE, QUOTE, from, to);
    values.forEach(this::save);
    recalculate();
    return new CollectionResult(BASE, QUOTE, from, to, values.size());
  }

  public List<Map<String, Object>> history(
      String requestedBase, String requestedQuote, LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("유효하지 않은 조회 기간입니다.");
    return jdbc.sql(
            """
   SELECT "BASE_DT" base_date,"BASE_CURR_CD" base_currency,"QUOTE_CURR_CD" quote_currency,
    "EXCH_RT" exchange_rate,"PREV_EXCH_RT" previous_exchange_rate,"CHG_AMT" change_amount,
    "CHG_RT" change_rate,"HIGH_52W_RT" high_52week_rate,"LOW_52W_RT" low_52week_rate,
    "PRESS_SCR" pressure_score,"DATA_SRC_CD" data_source_code,"DATA_STS" data_status,"COLLECT_DTTM" collected_at
   FROM "TB_EXCH_DAY" WHERE "BASE_CURR_CD"=:base AND "QUOTE_CURR_CD"=:quote
    AND "BASE_DT" BETWEEN :from AND :to ORDER BY "BASE_DT" DESC
   """)
        .param("base", currency(requestedBase))
        .param("quote", currency(requestedQuote))
        .param("from", from)
        .param("to", to)
        .query()
        .listOfRows();
  }

  private void save(ExchangeRateCollector.Quote value) {
    jdbc.sql(
            """
  INSERT INTO "TB_EXCH_DAY"("BASE_DT","BASE_CURR_CD","QUOTE_CURR_CD","EXCH_RT","DATA_SRC_CD","DATA_STS")
  VALUES(:day,:base,:quote,:rate,:source,'FRESH') ON CONFLICT("BASE_DT","BASE_CURR_CD","QUOTE_CURR_CD") DO UPDATE SET
   "EXCH_RT"=EXCLUDED."EXCH_RT","DATA_SRC_CD"=EXCLUDED."DATA_SRC_CD","DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP
  """)
        .param("day", value.baseDate())
        .param("base", value.baseCurrency())
        .param("quote", value.quoteCurrency())
        .param("rate", value.exchangeRate())
        .param("source", value.sourceCode())
        .update();
  }

  private void recalculate() {
    jdbc.sql(
            """
  WITH x AS (SELECT "EXCH_DAY_ID",lag("EXCH_RT") OVER(ORDER BY "BASE_DT") prev,
   max("EXCH_RT") OVER(ORDER BY "BASE_DT" ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) high52,
   min("EXCH_RT") OVER(ORDER BY "BASE_DT" ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) low52
   FROM "TB_EXCH_DAY" WHERE "BASE_CURR_CD"=:base AND "QUOTE_CURR_CD"=:quote)
  UPDATE "TB_EXCH_DAY" d SET "PREV_EXCH_RT"=x.prev,
   "CHG_AMT"=CASE WHEN x.prev IS NULL THEN NULL ELSE d."EXCH_RT"-x.prev END,
   "CHG_RT"=CASE WHEN x.prev IS NULL OR x.prev=0 THEN NULL ELSE round((d."EXCH_RT"-x.prev)*100/x.prev,4) END,
   "HIGH_52W_RT"=x.high52,"LOW_52W_RT"=x.low52 FROM x WHERE d."EXCH_DAY_ID"=x."EXCH_DAY_ID"
  """)
        .param("base", BASE)
        .param("quote", QUOTE)
        .update();
  }

  private String currency(String value) {
    String result = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    if (!result.matches("[A-Z]{3}"))
      throw new IllegalArgumentException("유효하지 않은 통화 코드입니다: " + value);
    return result;
  }

  public record CollectionResult(
      String baseCurrency, String quoteCurrency, LocalDate from, LocalDate to, int savedCount) {}
}
