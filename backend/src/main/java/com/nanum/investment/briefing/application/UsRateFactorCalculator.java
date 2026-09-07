package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class UsRateFactorCalculator {
  static final String FACTOR_CODE = "US_RATE";
  static final String METRIC_CODE = "US10Y_BP_CHANGE";
  static final String SERIES_CODE = "DGS10";
  static final int LOOKBACK_OBSERVATIONS = 252;
  private static final int MAX_STALE_CALENDAR_DAYS = 7;

  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public UsRateFactorCalculator(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Result calculate(LocalDate baseDate) {
    RateObservation current =
        jdbc.sql(
                """
                SELECT "BASE_DT", "YLD_RT", "CHG_BP", "DATA_SRC_CD"
                FROM "TB_FRED_BOND_DAY"
                WHERE "BOND_CD"=:seriesCode AND "BASE_DT"<=:baseDate
                  AND "CHG_BP" IS NOT NULL
                  AND "DATA_STS" IN ('FRESH','PARTIAL','STALE')
                ORDER BY "BASE_DT" DESC
                LIMIT 1
                """)
            .param("seriesCode", SERIES_CODE)
            .param("baseDate", baseDate)
            .query(
                (rs, rowNum) ->
                    new RateObservation(
                        rs.getObject(1, LocalDate.class),
                        rs.getBigDecimal(2),
                        rs.getBigDecimal(3),
                        rs.getString(4)))
            .optional()
            .orElse(null);

    if (current == null) {
      return Result.unavailable("MISSING", "기준일 이하의 미국 10년물 금리 변화가 없습니다.", 0);
    }

    List<RateObservation> history =
        jdbc.sql(
                """
                SELECT "BASE_DT", "YLD_RT", "CHG_BP", "DATA_SRC_CD"
                FROM "TB_FRED_BOND_DAY"
                WHERE "BOND_CD"=:seriesCode AND "BASE_DT"<:sourceDate
                  AND "CHG_BP" IS NOT NULL
                  AND "DATA_STS" IN ('FRESH','PARTIAL','STALE')
                ORDER BY "BASE_DT" DESC
                LIMIT :lookback
                """)
            .param("seriesCode", SERIES_CODE)
            .param("sourceDate", current.date())
            .param("lookback", LOOKBACK_OBSERVATIONS)
            .query(
                (rs, rowNum) ->
                    new RateObservation(
                        rs.getObject(1, LocalDate.class),
                        rs.getBigDecimal(2),
                        rs.getBigDecimal(3),
                        rs.getString(4)))
            .list();

    if (history.size() < LOOKBACK_OBSERVATIONS) {
      return Result.unavailable(
          "INSUFFICIENT_HISTORY",
          "미국 10년물 금리 변화 선행 이력이 "
              + history.size()
              + "건으로 최소 "
              + LOOKBACK_OBSERVATIONS
              + "건 미만입니다.",
          history.size());
    }
    if (ChronoUnit.DAYS.between(current.date(), baseDate) > MAX_STALE_CALENDAR_DAYS) {
      return Result.unavailable(
          "STALE", "최신 미국 10년물 금리 기준일이 " + current.date() + "로 오래되었습니다.", history.size());
    }

    RobustZScoreCalculator.Result normalized =
        RobustZScoreCalculator.calculate(
            current.changeBasisPoints().doubleValue(),
            history.stream().map(row -> row.changeBasisPoints().doubleValue()).toList());
    if (normalized.status() != RobustZScoreCalculator.Status.AVAILABLE) {
      return Result.unavailable("INVALID", normalized.reason(), history.size());
    }

    return new Result(
        "AVAILABLE",
        null,
        current.date(),
        current.yieldRate(),
        current.changeBasisPoints(),
        current.source(),
        history.size(),
        BigDecimal.valueOf(normalized.median()),
        BigDecimal.valueOf(normalized.medianAbsoluteDeviation()),
        BigDecimal.valueOf(normalized.rawZScore()),
        BigDecimal.valueOf(inverseScore(normalized.normalizedScore())),
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
            VALUES(:predictionId,:factorCode,:metricCode,'TB_FRED_BOND_DAY',CAST(:sourceKey AS jsonb),
              :sourceDate,:rawValue,:lookback,:sampleCount,:median,:mad,:robustZ,:score,100,100,
              :score,:status,'BP_CHANGE_ROBUST_Z_INVERSE_V1',CAST(:parameters AS jsonb),:sourceHash)
            """)
        .param("predictionId", predictionId)
        .param("factorCode", FACTOR_CODE)
        .param("metricCode", METRIC_CODE)
        .param(
            "sourceKey",
            toJson(Map.of("seriesCode", SERIES_CODE, "source", result.sourceCode())))
        .param("sourceDate", result.sourceDate())
        .param("rawValue", result.changeBasisPoints())
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
                    "yieldRate", result.yieldRate(),
                    "lookbackObservations", LOOKBACK_OBSERVATIONS,
                    "minimumObservations", LOOKBACK_OBSERVATIONS,
                    "zScoreLimit", 3.5,
                    "scoreFormula", "100 - clamp(50 + clamp(z,-3.5,3.5) * 10,0,100)",
                    "scoreDirection", "US10Y_BP_UP_IS_LOWER_SCORE")))
        .param("sourceHash", result.sourceHash())
        .update();
  }

  static double inverseScore(double normalizedScore) {
    return 100 - normalizedScore;
  }

  private String sourceHash(RateObservation current, List<RateObservation> history) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      history.stream()
          .sorted(Comparator.comparing(RateObservation::date))
          .forEach(row -> updateDigest(digest, row));
      updateDigest(digest, current);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
    }
  }

  private void updateDigest(MessageDigest digest, RateObservation row) {
    digest.update(
        (row.date()
                + "="
                + row.yieldRate().toPlainString()
                + ","
                + row.changeBasisPoints().toPlainString()
                + ","
                + row.source()
                + "\n")
            .getBytes(StandardCharsets.UTF_8));
  }

  private String toJson(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("미국 금리 Metric 근거를 JSON으로 변환할 수 없습니다.", e);
    }
  }

  private record RateObservation(
      LocalDate date, BigDecimal yieldRate, BigDecimal changeBasisPoints, String source) {}

  public record Result(
      String status,
      String reason,
      LocalDate sourceDate,
      BigDecimal yieldRate,
      BigDecimal changeBasisPoints,
      String sourceCode,
      int sampleCount,
      BigDecimal median,
      BigDecimal mad,
      BigDecimal robustZ,
      BigDecimal score,
      String sourceHash) {

    static Result unavailable(String status, String reason, int sampleCount) {
      return new Result(
          status, reason, null, null, null, null, sampleCount, null, null, null, null, null);
    }

    public boolean available() {
      return "AVAILABLE".equals(status);
    }
  }
}
