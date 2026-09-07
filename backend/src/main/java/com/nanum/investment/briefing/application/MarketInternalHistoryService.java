package com.nanum.investment.briefing.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketInternalHistoryService {
  static final String MARKET_CODE = "KOSPI";
  static final String SOURCE_DATASET = "KOSPI_STOCK_DAILY";
  static final String CALCULATION_VERSION = "KOSPI_INTERNAL_V1";

  private final JdbcClient jdbc;

  public MarketInternalHistoryService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public RebuildResult rebuild(LocalDate fromDate, LocalDate toDate) {
    if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
      throw new IllegalArgumentException("시장 내부강도 재계산 기간이 올바르지 않습니다.");
    }

    List<DailyInternal> rows = calculate(fromDate, toDate);
    rows.forEach(this::upsert);
    return new RebuildResult(
        MARKET_CODE,
        fromDate,
        toDate,
        rows.size(),
        rows.isEmpty() ? null : rows.get(0).baseDate(),
        rows.isEmpty() ? null : rows.get(rows.size() - 1).baseDate());
  }

  private List<DailyInternal> calculate(LocalDate fromDate, LocalDate toDate) {
    LocalDate windowStart = fromDate.minusDays(45);
    return jdbc.sql(
            """
            WITH source AS (
              SELECT "BASE_DT" AS base_dt,
                     "PAYLOAD"->>'ISU_CD' AS issue_code,
                     CASE WHEN ("PAYLOAD"->>'TDD_CLSPRC') ~ '^-?[0-9,]+([.][0-9]+)?$'
                          THEN replace("PAYLOAD"->>'TDD_CLSPRC', ',', '')::numeric END AS close_price,
                     CASE WHEN ("PAYLOAD"->>'CMPPREVDD_PRC') ~ '^-?[0-9,]+([.][0-9]+)?$'
                          THEN replace("PAYLOAD"->>'CMPPREVDD_PRC', ',', '')::numeric END AS change_price,
                     CASE WHEN ("PAYLOAD"->>'ACC_TRDVAL') ~ '^[0-9,]+([.][0-9]+)?$'
                          THEN replace("PAYLOAD"->>'ACC_TRDVAL', ',', '')::numeric END AS turnover
              FROM "TB_KRX_DATA_ROW"
              WHERE "DATA_CD"=:dataset AND "BASE_DT" BETWEEN :windowStart AND :toDate
                AND "PAYLOAD"->>'MKT_NM'=:market
            ), rolling AS (
              SELECT base_dt, issue_code, close_price, change_price, turnover,
                     count(close_price) OVER (
                       PARTITION BY issue_code ORDER BY base_dt
                       ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS ma20_samples,
                     avg(close_price) OVER (
                       PARTITION BY issue_code ORDER BY base_dt
                       ROWS BETWEEN 19 PRECEDING AND CURRENT ROW) AS ma20
              FROM source
              WHERE close_price>0
            )
            SELECT base_dt,
                   count(*)::int AS total_count,
                   count(*) FILTER (WHERE change_price>0)::int AS advance_count,
                   count(*) FILTER (WHERE change_price<0)::int AS decline_count,
                   count(*) FILTER (WHERE change_price=0)::int AS unchanged_count,
                   round(100.0 * count(*) FILTER (WHERE change_price>0)
                     / NULLIF(count(*) FILTER (WHERE change_price IS NOT NULL), 0), 8) AS advance_ratio,
                   count(*) FILTER (WHERE ma20_samples=20 AND close_price>ma20)::int AS above_ma20_count,
                   count(*) FILTER (WHERE ma20_samples=20)::int AS ma20_eligible_count,
                   round(100.0 * count(*) FILTER (WHERE ma20_samples=20 AND close_price>ma20)
                     / NULLIF(count(*) FILTER (WHERE ma20_samples=20), 0), 8) AS above_ma20_ratio,
                   round(100.0 * sum(CASE WHEN change_price>0 THEN turnover
                                          WHEN change_price<0 THEN -turnover ELSE 0 END)
                     / NULLIF(sum(turnover), 0), 8) AS signed_turnover_ratio,
                   count(*)::int AS source_row_count
            FROM rolling
            WHERE base_dt BETWEEN :fromDate AND :toDate
            GROUP BY base_dt
            ORDER BY base_dt
            """)
        .param("dataset", SOURCE_DATASET)
        .param("market", MARKET_CODE)
        .param("windowStart", windowStart)
        .param("fromDate", fromDate)
        .param("toDate", toDate)
        .query(
            (rs, rowNum) ->
                new DailyInternal(
                    rs.getObject("base_dt", LocalDate.class),
                    rs.getInt("total_count"),
                    rs.getInt("advance_count"),
                    rs.getInt("decline_count"),
                    rs.getInt("unchanged_count"),
                    rs.getBigDecimal("advance_ratio"),
                    rs.getInt("above_ma20_count"),
                    rs.getInt("ma20_eligible_count"),
                    rs.getBigDecimal("above_ma20_ratio"),
                    rs.getBigDecimal("signed_turnover_ratio"),
                    rs.getInt("source_row_count")))
        .list();
  }

  private void upsert(DailyInternal row) {
    jdbc.sql(
            """
            INSERT INTO "TB_MKT_INT_DAY"(
              "MKT_CD","BASE_DT","TOTAL_STK_CNT","ADV_STK_CNT","DECL_STK_CNT",
              "UNCH_STK_CNT","ADV_RATIO","ABOVE_MA20_CNT","MA20_ELIGIBLE_CNT",
              "ABOVE_MA20_RATIO","SIGNED_TURNOVER_RATIO","SOURCE_ROW_CNT","CALC_VER",
              "SOURCE_DATA_HASH")
            VALUES(:market,:baseDate,:total,:advance,:decline,:unchanged,:advanceRatio,
              :aboveMa20,:ma20Eligible,:aboveMa20Ratio,:signedTurnover,:sourceRows,:version,:sourceHash)
            ON CONFLICT ("MKT_CD","BASE_DT") DO UPDATE SET
              "TOTAL_STK_CNT"=EXCLUDED."TOTAL_STK_CNT", "ADV_STK_CNT"=EXCLUDED."ADV_STK_CNT",
              "DECL_STK_CNT"=EXCLUDED."DECL_STK_CNT", "UNCH_STK_CNT"=EXCLUDED."UNCH_STK_CNT",
              "ADV_RATIO"=EXCLUDED."ADV_RATIO", "ABOVE_MA20_CNT"=EXCLUDED."ABOVE_MA20_CNT",
              "MA20_ELIGIBLE_CNT"=EXCLUDED."MA20_ELIGIBLE_CNT",
              "ABOVE_MA20_RATIO"=EXCLUDED."ABOVE_MA20_RATIO",
              "SIGNED_TURNOVER_RATIO"=EXCLUDED."SIGNED_TURNOVER_RATIO",
              "SOURCE_ROW_CNT"=EXCLUDED."SOURCE_ROW_CNT", "CALC_VER"=EXCLUDED."CALC_VER",
              "SOURCE_DATA_HASH"=EXCLUDED."SOURCE_DATA_HASH", "MOD_DTTM"=CURRENT_TIMESTAMP
            """)
        .param("market", MARKET_CODE)
        .param("baseDate", row.baseDate())
        .param("total", row.totalCount())
        .param("advance", row.advanceCount())
        .param("decline", row.declineCount())
        .param("unchanged", row.unchangedCount())
        .param("advanceRatio", row.advanceRatio())
        .param("aboveMa20", row.aboveMa20Count())
        .param("ma20Eligible", row.ma20EligibleCount())
        .param("aboveMa20Ratio", row.aboveMa20Ratio())
        .param("signedTurnover", row.signedTurnoverRatio())
        .param("sourceRows", row.sourceRowCount())
        .param("version", CALCULATION_VERSION)
        .param("sourceHash", sourceHash(row))
        .update();
  }

  private static String sourceHash(DailyInternal row) {
    String value =
        row.baseDate()
            + "|"
            + row.totalCount()
            + "|"
            + row.advanceCount()
            + "|"
            + row.declineCount()
            + "|"
            + row.unchangedCount()
            + "|"
            + plain(row.advanceRatio())
            + "|"
            + plain(row.aboveMa20Ratio())
            + "|"
            + plain(row.signedTurnoverRatio());
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
    }
  }

  private static String plain(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  private record DailyInternal(
      LocalDate baseDate,
      int totalCount,
      int advanceCount,
      int declineCount,
      int unchangedCount,
      BigDecimal advanceRatio,
      int aboveMa20Count,
      int ma20EligibleCount,
      BigDecimal aboveMa20Ratio,
      BigDecimal signedTurnoverRatio,
      int sourceRowCount) {}

  public record RebuildResult(
      String marketCode,
      LocalDate requestedFromDate,
      LocalDate requestedToDate,
      int rebuiltDays,
      LocalDate firstBaseDate,
      LocalDate lastBaseDate) {}
}
