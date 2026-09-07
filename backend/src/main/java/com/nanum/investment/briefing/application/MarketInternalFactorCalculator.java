package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class MarketInternalFactorCalculator {
  static final String FACTOR_CODE = "MARKET_INTERNAL";
  private static final int MAX_STALE_CALENDAR_DAYS = 7;

  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public MarketInternalFactorCalculator(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Result calculate(LocalDate baseDate) {
    DailyInternal source = loadLatest(baseDate);
    if (source == null) {
      return Result.unavailable("MISSING", "기준일 이하의 KOSPI 시장 내부강도 이력이 없습니다.");
    }
    if (ChronoUnit.DAYS.between(source.baseDate(), baseDate) > MAX_STALE_CALENDAR_DAYS) {
      return Result.unavailable("STALE", "최신 KOSPI 시장 내부강도 기준일이 오래되었습니다.");
    }

    List<MetricResult> metrics =
        List.of(
            ratioMetric("ADVANCE_RATIO", source.advanceRatio(), 40, source),
            ratioMetric("ABOVE_MA20_RATIO", source.aboveMa20Ratio(), 40, source),
            signedRatioMetric(source.signedTurnoverRatio(), 20, source));
    List<MetricResult> available = metrics.stream().filter(MetricResult::available).toList();
    if (available.isEmpty()) {
      return new Result("INVALID", "계산 가능한 시장 내부강도 Metric이 없습니다.", null, 0, metrics);
    }

    BigDecimal availableWeight =
        available.stream().map(MetricResult::plannedWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal score =
        available.stream()
            .map(metric -> metric.score().multiply(metric.plannedWeight()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(availableWeight, 4, RoundingMode.HALF_UP);
    int confidence = availableWeight.intValue();
    String status = available.size() == metrics.size() ? "AVAILABLE" : "PARTIAL";
    String reason =
        "AVAILABLE".equals(status)
            ? null
            : "시장 내부강도 Metric " + metrics.size() + "개 중 " + available.size() + "개만 계산 가능합니다.";
    return new Result(status, reason, score, confidence, metrics);
  }

  public void saveMetrics(long predictionId, Result result) {
    BigDecimal availableWeight =
        result.metrics().stream()
            .filter(MetricResult::available)
            .map(MetricResult::plannedWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    for (MetricResult metric : result.metrics()) {
      BigDecimal effectiveWeight =
          metric.available()
              ? metric
                  .plannedWeight()
                  .multiply(BigDecimal.valueOf(100))
                  .divide(availableWeight, 4, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
      BigDecimal contribution =
          metric.available()
              ? metric
                  .score()
                  .multiply(effectiveWeight)
                  .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
              : null;
      jdbc.sql(
              """
              INSERT INTO "TB_MKT_DIR_PRED_METRIC"(
                "MKT_DIR_PRED_ID","FACTOR_CD","METRIC_CD","SOURCE_TABLE","SOURCE_KEY_JSON",
                "SOURCE_BASE_DT","RAW_VALUE","LOOKBACK_DAYS","SAMPLE_COUNT","METRIC_SCORE",
                "PLANNED_WEIGHT","EFFECTIVE_WEIGHT","CONTRIBUTION","DATA_STATUS","FORMULA_CD",
                "PARAM_JSON","SOURCE_DATA_HASH")
              VALUES(:predictionId,:factorCode,:metricCode,'TB_MKT_INT_DAY',CAST(:sourceKey AS jsonb),
                :sourceDate,:rawValue,:lookback,:sampleCount,:score,:plannedWeight,:effectiveWeight,
                :contribution,:status,:formula,CAST(:parameters AS jsonb),:sourceHash)
              """)
          .param("predictionId", predictionId)
          .param("factorCode", FACTOR_CODE)
          .param("metricCode", metric.metricCode())
          .param("sourceKey", toJson(Map.of("marketCode", MarketInternalHistoryService.MARKET_CODE)))
          .param("sourceDate", metric.sourceDate())
          .param("rawValue", metric.rawValue())
          .param("lookback", metric.lookbackDays())
          .param("sampleCount", metric.sampleCount())
          .param("score", metric.score())
          .param("plannedWeight", metric.plannedWeight())
          .param("effectiveWeight", effectiveWeight)
          .param("contribution", contribution)
          .param("status", metric.status())
          .param("formula", metric.formula())
          .param(
              "parameters",
              toJson(
                  Map.of(
                      "scoreDirection",
                      "HIGHER_INTERNAL_STRENGTH_IS_HIGHER_SCORE",
                      "calculationVersion",
                      MarketInternalHistoryService.CALCULATION_VERSION,
                      "reason",
                      metric.reason() == null ? "" : metric.reason())))
          .param("sourceHash", metric.sourceHash())
          .update();
    }
  }

  private DailyInternal loadLatest(LocalDate baseDate) {
    return jdbc.sql(
            """
            SELECT "BASE_DT","ADV_RATIO","ABOVE_MA20_RATIO","SIGNED_TURNOVER_RATIO",
                   "TOTAL_STK_CNT","MA20_ELIGIBLE_CNT","SOURCE_DATA_HASH"
            FROM "TB_MKT_INT_DAY"
            WHERE "MKT_CD"=:market AND "BASE_DT"<=:baseDate
            ORDER BY "BASE_DT" DESC
            LIMIT 1
            """)
        .param("market", MarketInternalHistoryService.MARKET_CODE)
        .param("baseDate", baseDate)
        .query(
            (rs, rowNum) ->
                new DailyInternal(
                    rs.getObject("BASE_DT", LocalDate.class),
                    rs.getBigDecimal("ADV_RATIO"),
                    rs.getBigDecimal("ABOVE_MA20_RATIO"),
                    rs.getBigDecimal("SIGNED_TURNOVER_RATIO"),
                    rs.getInt("TOTAL_STK_CNT"),
                    rs.getInt("MA20_ELIGIBLE_CNT"),
                    rs.getString("SOURCE_DATA_HASH")))
        .optional()
        .orElse(null);
  }

  private static MetricResult ratioMetric(
      String metricCode, BigDecimal rawValue, int weight, DailyInternal source) {
    if (rawValue == null) {
      return MetricResult.unavailable(metricCode, weight, "원천 비율이 없습니다.");
    }
    return MetricResult.available(
        metricCode,
        rawValue,
        clamp(rawValue),
        weight,
        "DIRECT_RATIO_V1",
        "ABOVE_MA20_RATIO".equals(metricCode) ? 20 : 1,
        "ABOVE_MA20_RATIO".equals(metricCode) ? source.ma20EligibleCount() : source.totalCount(),
        source);
  }

  private static MetricResult signedRatioMetric(BigDecimal rawValue, int weight, DailyInternal source) {
    if (rawValue == null) {
      return MetricResult.unavailable("SIGNED_TURNOVER_RATIO", weight, "거래대금 원천이 없습니다.");
    }
    BigDecimal score =
        clamp(rawValue.add(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP));
    return MetricResult.available(
        "SIGNED_TURNOVER_RATIO", rawValue, score, weight, "SIGNED_RATIO_TO_SCORE_V1", 1, source.totalCount(), source);
  }

  static BigDecimal clamp(BigDecimal value) {
    return value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));
  }

  private String toJson(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("시장 내부강도 Metric 근거를 JSON으로 변환할 수 없습니다.", e);
    }
  }

  private record DailyInternal(
      LocalDate baseDate,
      BigDecimal advanceRatio,
      BigDecimal aboveMa20Ratio,
      BigDecimal signedTurnoverRatio,
      int totalCount,
      int ma20EligibleCount,
      String sourceHash) {}

  public record Result(
      String status, String reason, BigDecimal score, int confidence, List<MetricResult> metrics) {
    static Result unavailable(String status, String reason) {
      return new Result(status, reason, null, 0, List.of());
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
      BigDecimal score,
      BigDecimal plannedWeight,
      String formula,
      int lookbackDays,
      int sampleCount,
      String sourceHash) {
    static MetricResult available(
        String metricCode,
        BigDecimal rawValue,
        BigDecimal score,
        int weight,
        String formula,
        int lookbackDays,
        int sampleCount,
        DailyInternal source) {
      return new MetricResult(
          metricCode,
          "AVAILABLE",
          null,
          source.baseDate(),
          rawValue,
          score,
          BigDecimal.valueOf(weight),
          formula,
          lookbackDays,
          sampleCount,
          source.sourceHash());
    }

    static MetricResult unavailable(String metricCode, int weight, String reason) {
      return new MetricResult(
          metricCode,
          "MISSING",
          reason,
          null,
          null,
          null,
          BigDecimal.valueOf(weight),
          "NOT_CALCULATED",
          1,
          0,
          null);
    }

    public boolean available() {
      return "AVAILABLE".equals(status);
    }
  }
}
