package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SemiconductorFactorCalculator {
  static final String FACTOR_CODE = "SEMICONDUCTOR";
  static final String SOX_INDEX_CODE = "PHLX_SEMICONDUCTOR";
  static final String NASDAQ_INDEX_CODE = "NASDAQ_COMPOSITE";
  static final int RETURN_HORIZON = 20;
  static final int LOOKBACK_OBSERVATIONS = 252;
  private static final int REQUIRED_SOURCE_VALUES = RETURN_HORIZON + LOOKBACK_OBSERVATIONS + 1;
  private static final int MAX_STALE_CALENDAR_DAYS = 7;

  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public SemiconductorFactorCalculator(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Result calculate(LocalDate baseDate) {
    List<IndexPair> observations = loadObservations(baseDate);
    if (observations.isEmpty()) {
      return Result.unavailable("MISSING", "기준일 이하의 SOX·NASDAQ 공통 관측치가 없습니다.", 0);
    }
    if (ChronoUnit.DAYS.between(observations.get(0).date(), baseDate)
        > MAX_STALE_CALENDAR_DAYS) {
      return Result.unavailable(
          "STALE", "최신 SOX·NASDAQ 공통 기준일이 오래되었습니다.", observations.size());
    }
    if (observations.size() < REQUIRED_SOURCE_VALUES) {
      return Result.unavailable(
          "INSUFFICIENT_HISTORY",
          "20일 반도체 수익률 분포 계산에 " + REQUIRED_SOURCE_VALUES + "개 공통 관측치가 필요합니다.",
          observations.size());
    }

    List<Double> soxHistory = new ArrayList<>(LOOKBACK_OBSERVATIONS);
    List<Double> relativeHistory = new ArrayList<>(LOOKBACK_OBSERVATIONS);
    for (int offset = 1; offset <= LOOKBACK_OBSERVATIONS; offset++) {
      double soxReturn = returnRate(observations.get(offset).sox(), observations.get(offset + RETURN_HORIZON).sox());
      double nasdaqReturn =
          returnRate(
              observations.get(offset).nasdaq(),
              observations.get(offset + RETURN_HORIZON).nasdaq());
      soxHistory.add(soxReturn);
      relativeHistory.add(relativeStrength(soxReturn, nasdaqReturn));
    }

    double currentSoxReturn =
        returnRate(observations.get(0).sox(), observations.get(RETURN_HORIZON).sox());
    double currentNasdaqReturn =
        returnRate(observations.get(0).nasdaq(), observations.get(RETURN_HORIZON).nasdaq());
    double currentRelative = relativeStrength(currentSoxReturn, currentNasdaqReturn);
    MetricResult soxMetric =
        normalize("SOX_RETURN_20D", currentSoxReturn, soxHistory, observations);
    MetricResult relativeMetric =
        normalize("SOX_NASDAQ_RELATIVE_20D", currentRelative, relativeHistory, observations);
    List<MetricResult> metrics = List.of(soxMetric, relativeMetric);
    List<MetricResult> available = metrics.stream().filter(MetricResult::available).toList();
    if (available.isEmpty()) {
      return new Result("INVALID", "계산 가능한 반도체 Metric이 없습니다.", null, 0, metrics);
    }

    BigDecimal score =
        available.stream()
            .map(MetricResult::score)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(available.size()), 4, RoundingMode.HALF_UP);
    int confidence = available.size() * 100 / metrics.size();
    String status = available.size() == metrics.size() ? "AVAILABLE" : "PARTIAL";
    String reason =
        "AVAILABLE".equals(status)
            ? null
            : "반도체 Metric " + metrics.size() + "개 중 " + available.size() + "개만 계산 가능합니다.";
    return new Result(status, reason, score, confidence, metrics);
  }

  public void saveMetrics(long predictionId, Result result) {
    int availableCount = (int) result.metrics().stream().filter(MetricResult::available).count();
    BigDecimal effectiveWeight =
        availableCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(availableCount), 4, RoundingMode.HALF_UP);
    for (MetricResult metric : result.metrics()) {
      BigDecimal appliedWeight = metric.available() ? effectiveWeight : BigDecimal.ZERO;
      BigDecimal contribution =
          metric.available()
              ? metric
                  .score()
                  .multiply(appliedWeight)
                  .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
              : null;
      jdbc.sql(
              """
              INSERT INTO "TB_MKT_DIR_PRED_METRIC"(
                "MKT_DIR_PRED_ID","FACTOR_CD","METRIC_CD","SOURCE_TABLE","SOURCE_KEY_JSON",
                "SOURCE_BASE_DT","RAW_VALUE","LOOKBACK_DAYS","SAMPLE_COUNT","ROLLING_MEDIAN",
                "ROLLING_MAD","ROBUST_Z","METRIC_SCORE","PLANNED_WEIGHT","EFFECTIVE_WEIGHT",
                "CONTRIBUTION","DATA_STATUS","FORMULA_CD","PARAM_JSON","SOURCE_DATA_HASH")
              VALUES(:predictionId,:factorCode,:metricCode,'TB_IDX_DAY',CAST(:sourceKey AS jsonb),
                :sourceDate,:rawValue,:lookback,:sampleCount,:median,:mad,:robustZ,:score,50,
                :effectiveWeight,:contribution,:status,'RETURN_20D_ROBUST_Z_POSITIVE_V1',
                CAST(:parameters AS jsonb),:sourceHash)
              """)
          .param("predictionId", predictionId)
          .param("factorCode", FACTOR_CODE)
          .param("metricCode", metric.metricCode())
          .param(
              "sourceKey",
              toJson(
                  Map.of(
                      "soxIndexCode", SOX_INDEX_CODE,
                      "benchmarkIndexCode", NASDAQ_INDEX_CODE)))
          .param("sourceDate", metric.sourceDate())
          .param("rawValue", metric.rawValue())
          .param("lookback", LOOKBACK_OBSERVATIONS)
          .param("sampleCount", metric.sampleCount())
          .param("median", metric.median())
          .param("mad", metric.mad())
          .param("robustZ", metric.robustZ())
          .param("score", metric.score())
          .param("effectiveWeight", appliedWeight)
          .param("contribution", contribution)
          .param("status", metric.status())
          .param(
              "parameters",
              toJson(
                  Map.of(
                      "returnHorizonObservations", RETURN_HORIZON,
                      "lookbackObservations", LOOKBACK_OBSERVATIONS,
                      "scoreDirection", "HIGHER_RETURN_IS_HIGHER_SCORE",
                      "relativeFormula", "SOX_RETURN_20D - NASDAQ_RETURN_20D",
                      "reason", metric.reason() == null ? "" : metric.reason())))
          .param("sourceHash", metric.sourceHash())
          .update();
    }
  }

  private List<IndexPair> loadObservations(LocalDate baseDate) {
    return jdbc.sql(
            """
            SELECT sox."TRADE_DT", sox."CLS_VAL", nasdaq."CLS_VAL"
            FROM "TB_IDX_DAY" sox
            JOIN "TB_IDX_DAY" nasdaq ON nasdaq."TRADE_DT"=sox."TRADE_DT"
            WHERE sox."IDX_CD"=:soxCode AND nasdaq."IDX_CD"=:nasdaqCode
              AND sox."TRADE_DT"<=:baseDate
              AND sox."CLS_VAL">0 AND nasdaq."CLS_VAL">0
              AND sox."DATA_STS" IN ('FRESH','PARTIAL','STALE')
              AND nasdaq."DATA_STS" IN ('FRESH','PARTIAL','STALE')
            ORDER BY sox."TRADE_DT" DESC
            LIMIT :requiredValues
            """)
        .param("soxCode", SOX_INDEX_CODE)
        .param("nasdaqCode", NASDAQ_INDEX_CODE)
        .param("baseDate", baseDate)
        .param("requiredValues", REQUIRED_SOURCE_VALUES)
        .query(
            (rs, rowNum) ->
                new IndexPair(
                    rs.getObject(1, LocalDate.class), rs.getBigDecimal(2), rs.getBigDecimal(3)))
        .list();
  }

  private MetricResult normalize(
      String metricCode, double currentValue, List<Double> history, List<IndexPair> source) {
    RobustZScoreCalculator.Result normalized =
        RobustZScoreCalculator.calculate(currentValue, history);
    if (normalized.status() != RobustZScoreCalculator.Status.AVAILABLE) {
      return MetricResult.unavailable(
          metricCode, "INVALID", normalized.reason(), normalized.sampleCount());
    }
    return new MetricResult(
        metricCode,
        "AVAILABLE",
        null,
        source.get(0).date(),
        BigDecimal.valueOf(currentValue),
        normalized.sampleCount(),
        BigDecimal.valueOf(normalized.median()),
        BigDecimal.valueOf(normalized.medianAbsoluteDeviation()),
        BigDecimal.valueOf(normalized.rawZScore()),
        BigDecimal.valueOf(normalized.normalizedScore()),
        sourceHash(source));
  }

  static double returnRate(BigDecimal current, BigDecimal previous) {
    return current.subtract(previous).divide(previous, 12, RoundingMode.HALF_UP).doubleValue() * 100;
  }

  static double relativeStrength(double assetReturn, double benchmarkReturn) {
    return assetReturn - benchmarkReturn;
  }

  private String sourceHash(List<IndexPair> observations) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      observations.stream()
          .sorted(Comparator.comparing(IndexPair::date))
          .forEach(
              row ->
                  digest.update(
                      (row.date()
                              + "="
                              + row.sox().toPlainString()
                              + ","
                              + row.nasdaq().toPlainString()
                              + "\n")
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
      throw new IllegalStateException("반도체 Metric 근거를 JSON으로 변환할 수 없습니다.", e);
    }
  }

  private record IndexPair(LocalDate date, BigDecimal sox, BigDecimal nasdaq) {}

  public record Result(
      String status, String reason, BigDecimal score, int confidence, List<MetricResult> metrics) {
    static Result unavailable(String status, String reason, int sampleCount) {
      return new Result(
          status,
          reason,
          null,
          0,
          List.of(
              MetricResult.unavailable("SOX_RETURN_20D", status, reason, sampleCount),
              MetricResult.unavailable(
                  "SOX_NASDAQ_RELATIVE_20D", status, reason, sampleCount)));
    }

    public boolean available() {
      return "AVAILABLE".equals(status) || "PARTIAL".equals(status);
    }
  }

  public record MetricResult(
      String metricCode,
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
    static MetricResult unavailable(
        String metricCode, String status, String reason, int sampleCount) {
      return new MetricResult(
          metricCode, status, reason, null, null, sampleCount, null, null, null, null, null);
    }

    public boolean available() {
      return "AVAILABLE".equals(status);
    }
  }
}
