package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class FxDollarFactorCalculator {
  static final String FACTOR_CODE = "FX_DOLLAR";
  static final String METRIC_CODE = "USD_KRW_LEVEL";
  static final int LOOKBACK_OBSERVATIONS = 252;
  private static final int MAX_STALE_CALENDAR_DAYS = 7;

  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public FxDollarFactorCalculator(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Result calculate(LocalDate baseDate) {
    ExchangeObservation current =
        jdbc.sql(
                """
                SELECT "BASE_DT", "EXCH_RT"
                FROM "TB_EXCH_DAY"
                WHERE "BASE_CURR_CD"='USD' AND "QUOTE_CURR_CD"='KRW'
                  AND "BASE_DT"<=:baseDate
                  AND "DATA_STS" IN ('FRESH','PARTIAL','STALE')
                ORDER BY "BASE_DT" DESC
                LIMIT 1
                """)
            .param("baseDate", baseDate)
            .query(
                (rs, rowNum) ->
                    new ExchangeObservation(
                        rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
            .optional()
            .orElse(null);

    if (current == null) {
      return Result.unavailable("MISSING", "기준일 이하의 USD/KRW 환율이 없습니다.", 0);
    }

    List<ExchangeObservation> history =
        jdbc.sql(
                """
                SELECT "BASE_DT", "EXCH_RT"
                FROM "TB_EXCH_DAY"
                WHERE "BASE_CURR_CD"='USD' AND "QUOTE_CURR_CD"='KRW'
                  AND "BASE_DT"<:sourceDate
                  AND "DATA_STS" IN ('FRESH','PARTIAL','STALE')
                ORDER BY "BASE_DT" DESC
                LIMIT :lookback
                """)
            .param("sourceDate", current.date())
            .param("lookback", LOOKBACK_OBSERVATIONS)
            .query(
                (rs, rowNum) ->
                    new ExchangeObservation(
                        rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
            .list();

    if (history.size() < LOOKBACK_OBSERVATIONS) {
      return Result.unavailable(
          "INSUFFICIENT_HISTORY",
          "USD/KRW 선행 이력이 " + history.size() + "건으로 최소 " + LOOKBACK_OBSERVATIONS + "건 미만입니다.",
          history.size());
    }
    if (ChronoUnit.DAYS.between(current.date(), baseDate) > MAX_STALE_CALENDAR_DAYS) {
      return Result.unavailable(
          "STALE", "최신 USD/KRW 환율 기준일이 " + current.date() + "로 오래되었습니다.", history.size());
    }

    RobustZScoreCalculator.Result metric =
        RobustZScoreCalculator.calculate(
            current.rate().doubleValue(), history.stream().map(row -> row.rate().doubleValue()).toList());
    if (metric.status() != RobustZScoreCalculator.Status.AVAILABLE) {
      return Result.unavailable("INVALID", metric.reason(), history.size());
    }

    return new Result(
        "AVAILABLE",
        null,
        current.date(),
        current.rate(),
        history.size(),
        BigDecimal.valueOf(metric.median()),
        BigDecimal.valueOf(metric.medianAbsoluteDeviation()),
        BigDecimal.valueOf(metric.rawZScore()),
        BigDecimal.valueOf(metric.normalizedScore()),
        sourceHash(current, history));
  }

  public void saveMetric(long predictionId, Result result) {
    jdbc.sql(
            """
            INSERT INTO "TB_MKT_DIR_PRED_METRIC"(
              "MKT_DIR_PRED_ID","FACTOR_CD","METRIC_CD","SOURCE_TABLE","SOURCE_KEY_JSON",
              "SOURCE_BASE_DT","RAW_VALUE","LOOKBACK_DAYS","SAMPLE_COUNT","ROLLING_MEDIAN",
              "ROLLING_MAD","ROBUST_Z","METRIC_SCORE","PLANNED_WEIGHT","EFFECTIVE_WEIGHT",
              "CONTRIBUTION","DATA_STATUS","FORMULA_CD","PARAM_JSON","SOURCE_DATA_HASH")
            VALUES(:predictionId,:factorCode,:metricCode,'TB_EXCH_DAY',CAST(:sourceKey AS jsonb),
              :sourceDate,:rawValue,:lookback,:sampleCount,:median,:mad,:robustZ,:score,100,100,
              :score,:status,'ROBUST_Z_MAD_V1',CAST(:parameters AS jsonb),:sourceHash)
            """)
        .param("predictionId", predictionId)
        .param("factorCode", FACTOR_CODE)
        .param("metricCode", METRIC_CODE)
        .param("sourceKey", toJson(Map.of("baseCurrency", "USD", "quoteCurrency", "KRW")))
        .param("sourceDate", result.sourceDate())
        .param("rawValue", result.rawValue())
        .param("lookback", LOOKBACK_OBSERVATIONS)
        .param("sampleCount", result.sampleCount())
        .param("median", result.median())
        .param("mad", result.mad())
        .param("robustZ", result.robustZ())
        .param("score", result.score())
        .param("status", result.status())
        .param(
            "parameters",
            toJson(
                Map.of(
                    "lookbackObservations", LOOKBACK_OBSERVATIONS,
                    "minimumObservations", LOOKBACK_OBSERVATIONS,
                    "zScoreLimit", 3.5,
                    "scoreFormula", "clamp(50 + clamp(z,-3.5,3.5) * 10,0,100)",
                    "riskDirection", "USD_KRW_UP_IS_HIGHER_RISK")))
        .param("sourceHash", result.sourceHash())
        .update();
  }

  private String sourceHash(ExchangeObservation current, List<ExchangeObservation> history) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(
          (current.date() + "=" + current.rate().toPlainString() + "\n")
              .getBytes(StandardCharsets.UTF_8));
      history.stream()
          .sorted(java.util.Comparator.comparing(ExchangeObservation::date))
          .forEach(
              row ->
                  digest.update(
                      (row.date() + "=" + row.rate().toPlainString() + "\n")
                          .getBytes(StandardCharsets.UTF_8)));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
    }
  }

  private String toJson(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("환율 Metric 근거를 JSON으로 변환할 수 없습니다.", e);
    }
  }

  private record ExchangeObservation(LocalDate date, BigDecimal rate) {}

  public record Result(
      String status,
      String reason,
      LocalDate sourceDate,
      BigDecimal rawValue,
      int sampleCount,
      BigDecimal median,
      BigDecimal mad,
      BigDecimal robustZ,
      BigDecimal score,
      String sourceHash) {

    static Result unavailable(String status, String reason, int sampleCount) {
      return new Result(status, reason, null, null, sampleCount, null, null, null, null, null);
    }

    public boolean available() {
      return "AVAILABLE".equals(status);
    }
  }
}
