package com.nanum.investment.briefing.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class AlgorithmPerformanceService {
  private final JdbcClient jdbc;

  public AlgorithmPerformanceService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public PerformanceComparison compare(
      LocalDate from,
      LocalDate to,
      String target,
      int horizon,
      BigDecimal warningThreshold,
      BigDecimal drawdownThreshold) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("올바른 성능평가 시작일과 종료일이 필요합니다.");
    if (!List.of("KOSPI", "KOSDAQ", "SP500").contains(target))
      throw new IllegalArgumentException("target은 KOSPI, KOSDAQ, SP500 중 하나여야 합니다.");
    if (!List.of(1, 5, 10, 20, 60).contains(horizon))
      throw new IllegalArgumentException("horizon은 1, 5, 10, 20, 60 중 하나여야 합니다.");
    List<Observation> observations = observations(from, to, target, horizon);
    Metrics legacy = metrics("LEGACY_CHATGPT", observations, warningThreshold, drawdownThreshold, true);
    Metrics recalculated =
        metrics("MARKET_ALGORITHM_V2", observations, warningThreshold, drawdownThreshold, false);
    String assessment = assessment(legacy, recalculated);
    return new PerformanceComparison(
        from,
        to,
        target,
        horizon,
        warningThreshold,
        drawdownThreshold,
        "DATASET_V1",
        legacy,
        recalculated,
        assessment);
  }

  private List<Observation> observations(LocalDate from, LocalDate to, String target, int horizon) {
    return jdbc.sql(
            """
            SELECT c."MKT_BASE_DT",e."MKT_RISK_SCR",
                   NULLIF(r."RESULT_JSON"->>'marketRiskScore','')::numeric,
                   o."RETURN_RT",o."MAX_DRAWDOWN_RT",r."ALGORITHM_VER",r."DATASET_VER"
              FROM "TB_BRF_CASE" c
              LEFT JOIN LATERAL (
                SELECT x."MKT_RISK_SCR" FROM "TB_LEGACY_BRF" l
                JOIN "TB_LEGACY_BRF_EXTRACT" x ON x."LEGACY_BRF_ID"=l."LEGACY_BRF_ID" AND x."EXTRACT_VER"=1
                WHERE l."CASE_ID"=c."CASE_ID" ORDER BY l."GENERATED_DTTM" LIMIT 1
              ) e ON true
              LEFT JOIN LATERAL (
                SELECT x.* FROM "TB_HIST_RECALC_RUN" x WHERE x."CASE_ID"=c."CASE_ID" AND x."RUN_STS"='SUCCESS'
                ORDER BY x."START_DTTM" DESC LIMIT 1
              ) r ON true
              JOIN LATERAL (
                SELECT x."RETURN_RT",x."MAX_DRAWDOWN_RT" FROM "TB_BRF_EVAL" x
                 WHERE x."CASE_ID"=c."CASE_ID" AND x."TARGET_CD"=:target AND x."HORIZON_BD"=:horizon
                   AND x."EVAL_STS"='COMPLETE' ORDER BY x."CRT_DTTM" DESC LIMIT 1
              ) o ON true
             WHERE c."MKT_BASE_DT" BETWEEN :from AND :to ORDER BY c."MKT_BASE_DT"
            """)
        .param("from", from)
        .param("to", to)
        .param("target", target)
        .param("horizon", horizon)
        .query(
            (rs, n) ->
                new Observation(
                    rs.getObject(1, LocalDate.class),
                    rs.getBigDecimal(2),
                    rs.getBigDecimal(3),
                    rs.getBigDecimal(4),
                    rs.getBigDecimal(5),
                    rs.getString(6),
                    rs.getString(7)))
        .list();
  }

  private Metrics metrics(
      String model,
      List<Observation> rows,
      BigDecimal warningThreshold,
      BigDecimal drawdownThreshold,
      boolean legacy) {
    int tp = 0, fp = 0, tn = 0, fn = 0;
    BigDecimal riskTotal = BigDecimal.ZERO;
    BigDecimal returnTotal = BigDecimal.ZERO;
    BigDecimal drawdownTotal = BigDecimal.ZERO;
    int evaluated = 0;
    for (Observation row : rows) {
      BigDecimal risk = legacy ? row.legacyRisk() : row.recalculatedRisk();
      if (risk == null) continue;
      boolean warning = risk.compareTo(warningThreshold) >= 0;
      boolean downside = row.maxDrawdown().compareTo(drawdownThreshold) <= 0;
      if (warning && downside) tp++;
      else if (warning) fp++;
      else if (downside) fn++;
      else tn++;
      evaluated++;
      riskTotal = riskTotal.add(risk);
      returnTotal = returnTotal.add(row.returnRate());
      drawdownTotal = drawdownTotal.add(row.maxDrawdown());
    }
    return new Metrics(
        model,
        evaluated,
        tp + fp,
        tp + fn,
        tp,
        fp,
        tn,
        fn,
        ratio(tp, tp + fp),
        ratio(tp, tp + fn),
        ratio(tp + tn, evaluated),
        average(riskTotal, evaluated),
        average(returnTotal, evaluated),
        average(drawdownTotal, evaluated));
  }

  private BigDecimal ratio(int numerator, int denominator) {
    return denominator == 0
        ? null
        : BigDecimal.valueOf(numerator * 100L)
            .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
  }

  private BigDecimal average(BigDecimal total, int count) {
    return count == 0 ? null : total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
  }

  private String assessment(Metrics legacy, Metrics recalc) {
    if (legacy.evaluatedCases() == 0 || recalc.evaluatedCases() == 0) return "INSUFFICIENT_DATA";
    int recall = compareNullable(recalc.recallRate(), legacy.recallRate());
    if (recall > 0) return "RECALC_HIGHER_RECALL";
    if (recall < 0) return "LEGACY_HIGHER_RECALL";
    int precision = compareNullable(recalc.precisionRate(), legacy.precisionRate());
    if (precision > 0) return "RECALC_HIGHER_PRECISION";
    if (precision < 0) return "LEGACY_HIGHER_PRECISION";
    return "SAME_AT_THRESHOLDS";
  }

  private int compareNullable(BigDecimal left, BigDecimal right) {
    if (left == null && right == null) return 0;
    if (left == null) return -1;
    if (right == null) return 1;
    return left.compareTo(right);
  }

  private record Observation(
      LocalDate baseDate,
      BigDecimal legacyRisk,
      BigDecimal recalculatedRisk,
      BigDecimal returnRate,
      BigDecimal maxDrawdown,
      String algorithmVersion,
      String datasetVersion) {}

  public record PerformanceComparison(
      LocalDate from,
      LocalDate to,
      String target,
      int horizonBusinessDays,
      BigDecimal warningThreshold,
      BigDecimal drawdownThreshold,
      String datasetVersion,
      Metrics legacy,
      Metrics recalculated,
      String assessment) {}

  public record Metrics(
      String model,
      int evaluatedCases,
      int warnings,
      int downsideEvents,
      int truePositive,
      int falsePositive,
      int trueNegative,
      int falseNegative,
      BigDecimal precisionRate,
      BigDecimal recallRate,
      BigDecimal accuracyRate,
      BigDecimal averageRiskScore,
      BigDecimal averageReturnRate,
      BigDecimal averageMaxDrawdownRate) {}
}
