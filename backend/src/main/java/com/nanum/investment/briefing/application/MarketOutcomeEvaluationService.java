package com.nanum.investment.briefing.application;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketOutcomeEvaluationService {
  private static final List<String> TARGETS = List.of("KOSPI", "KOSDAQ", "SP500");
  private static final List<Integer> HORIZONS = List.of(1, 5, 10, 20, 60);
  private static final String EVALUATION_VERSION = "OUTCOME_V1";
  private static final String DATA_VERSION = "HIST_20260830_V1";
  private final JdbcClient jdbc;

  public MarketOutcomeEvaluationService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public BatchResult evaluate(LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("올바른 평가 시작일과 종료일이 필요합니다.");
    List<CaseRow> cases = cases(from, to);
    int complete = 0, notAvailable = 0;
    for (CaseRow item : cases) {
      for (String target : TARGETS) {
        List<Price> prices = prices(target, item.baseDate());
        for (int horizon : HORIZONS) {
          Evaluation value = calculate(prices, horizon);
          save(item, target, horizon, value);
          if (value.complete()) complete++;
          else notAvailable++;
        }
      }
    }
    return new BatchResult(
        cases.size(), cases.size() * TARGETS.size() * HORIZONS.size(), complete, notAvailable);
  }

  private Evaluation calculate(List<Price> prices, int horizon) {
    if (prices.size() <= horizon) return Evaluation.notAvailable();
    List<Price> window = prices.subList(0, horizon + 1);
    BigDecimal base = window.getFirst().value(), end = window.getLast().value();
    BigDecimal totalReturn = rate(end, base);
    BigDecimal peak = base, maxDrawdown = BigDecimal.ZERO, maxRunup = BigDecimal.ZERO;
    List<Double> dailyReturns = new ArrayList<>();
    for (int index = 1; index < window.size(); index++) {
      BigDecimal value = window.get(index).value();
      if (value.compareTo(peak) > 0) peak = value;
      BigDecimal drawdown = rate(value, peak);
      if (drawdown.compareTo(maxDrawdown) < 0) maxDrawdown = drawdown;
      BigDecimal runup = rate(value, base);
      if (runup.compareTo(maxRunup) > 0) maxRunup = runup;
      dailyReturns.add(
          value.divide(window.get(index - 1).value(), MathContext.DECIMAL64).doubleValue() - 1d);
    }
    BigDecimal volatility =
        BigDecimal.valueOf(annualizedVolatility(dailyReturns)).setScale(6, RoundingMode.HALF_UP);
    return new Evaluation(
        true,
        base,
        end,
        window.getLast().date(),
        totalReturn,
        maxDrawdown,
        maxRunup,
        volatility,
        outcomeClass(totalReturn));
  }

  private double annualizedVolatility(List<Double> values) {
    if (values.size() < 2) return 0d;
    double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
    double variance =
        values.stream().mapToDouble(value -> Math.pow(value - mean, 2)).sum() / (values.size() - 1);
    return Math.sqrt(variance) * Math.sqrt(252d) * 100d;
  }

  private BigDecimal rate(BigDecimal value, BigDecimal base) {
    return value
        .subtract(base)
        .multiply(BigDecimal.valueOf(100))
        .divide(base, 6, RoundingMode.HALF_UP);
  }

  private String outcomeClass(BigDecimal value) {
    if (value.compareTo(BigDecimal.valueOf(5)) >= 0) return "STRONG_UP";
    if (value.signum() > 0) return "UP";
    if (value.compareTo(BigDecimal.valueOf(-5)) <= 0) return "STRONG_DOWN";
    if (value.signum() < 0) return "DOWN";
    return "FLAT";
  }

  private List<CaseRow> cases(LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            SELECT c."CASE_ID",c."MKT_BASE_DT",
                   (SELECT l."LEGACY_BRF_ID" FROM "TB_LEGACY_BRF" l WHERE l."CASE_ID"=c."CASE_ID" ORDER BY l."GENERATED_DTTM" LIMIT 1),
                   (SELECT r."HIST_RECALC_RUN_ID" FROM "TB_HIST_RECALC_RUN" r WHERE r."CASE_ID"=c."CASE_ID" AND r."RUN_STS"='SUCCESS' ORDER BY r."START_DTTM" DESC LIMIT 1)
              FROM "TB_BRF_CASE" c WHERE c."MKT_BASE_DT" BETWEEN :from AND :to ORDER BY c."MKT_BASE_DT"
            """)
        .param("from", from)
        .param("to", to)
        .query(
            (rs, n) ->
                new CaseRow(
                    rs.getLong(1),
                    rs.getObject(2, LocalDate.class),
                    (Long) rs.getObject(3),
                    (Long) rs.getObject(4)))
        .list();
  }

  private List<Price> prices(String target, LocalDate baseDate) {
    if ("SP500".equals(target)) {
      return jdbc.sql(
              """
              WITH base AS (SELECT max("TRADE_DT") d FROM "TB_IDX_DAY" WHERE "IDX_CD"='SP500' AND "TRADE_DT"<=:day)
              SELECT "TRADE_DT","CLS_VAL" FROM "TB_IDX_DAY",base
               WHERE "IDX_CD"='SP500' AND "TRADE_DT">=base.d ORDER BY "TRADE_DT" LIMIT 61
              """)
          .param("day", baseDate)
          .query((rs, n) -> new Price(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
          .list();
    }
    return jdbc.sql(
            """
            WITH base AS (SELECT max(base_date) d FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date<=:day),
            points AS (
              SELECT DISTINCT ON (base_date) base_date,NULLIF(replace(payload->>'CLSPRC_IDX',',',''),'')::numeric value
                FROM tb_krx_data_row,base WHERE dataset_code=:dataset AND base_date>=base.d
                 AND payload->>'IDX_NM' IN (:koreanName,:target) ORDER BY base_date,payload->>'IDX_NM'
            ) SELECT base_date,value FROM points WHERE value IS NOT NULL ORDER BY base_date LIMIT 61
            """)
        .param("dataset", target + "_INDEX_DAILY")
        .param("day", baseDate)
        .param("koreanName", "KOSPI".equals(target) ? "코스피" : "코스닥")
        .param("target", target)
        .query((rs, n) -> new Price(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
        .list();
  }

  private void save(CaseRow item, String target, int horizon, Evaluation value) {
    jdbc.sql(
            """
            INSERT INTO "TB_BRF_EVAL"("CASE_ID","LEGACY_BRF_ID","HIST_RECALC_RUN_ID","BASE_DT","TARGET_CD","HORIZON_BD",
             "BASE_PRC","END_PRC","END_DT","RETURN_RT","MAX_DRAWDOWN_RT","MAX_RUNUP_RT","VOLATILITY_RT","OUTCOME_CLASS",
             "EVAL_STS","EVAL_RULE_VER","DATA_VER","DETAIL_JSON")
            VALUES(:caseId,:legacyId,:runId,:baseDate,:target,:horizon,:basePrice,:endPrice,:endDate,:returnRate,:drawdown,
             :runup,:volatility,:outcomeClass,:status,:evaluationVersion,:dataVersion,'{}'::jsonb)
            ON CONFLICT("LEGACY_BRF_ID","HIST_RECALC_RUN_ID","TARGET_CD","HORIZON_BD","EVAL_RULE_VER") DO UPDATE SET
             "CASE_ID"=EXCLUDED."CASE_ID","BASE_PRC"=EXCLUDED."BASE_PRC","END_PRC"=EXCLUDED."END_PRC","END_DT"=EXCLUDED."END_DT",
             "RETURN_RT"=EXCLUDED."RETURN_RT","MAX_DRAWDOWN_RT"=EXCLUDED."MAX_DRAWDOWN_RT","MAX_RUNUP_RT"=EXCLUDED."MAX_RUNUP_RT",
             "VOLATILITY_RT"=EXCLUDED."VOLATILITY_RT","OUTCOME_CLASS"=EXCLUDED."OUTCOME_CLASS","EVAL_STS"=EXCLUDED."EVAL_STS",
             "DATA_VER"=EXCLUDED."DATA_VER","CRT_DTTM"=CURRENT_TIMESTAMP
            """)
        .param("caseId", item.caseId())
        .param("legacyId", item.legacyId())
        .param("runId", item.runId())
        .param("baseDate", item.baseDate())
        .param("target", target)
        .param("horizon", horizon)
        .param("basePrice", value.basePrice())
        .param("endPrice", value.endPrice())
        .param("endDate", value.endDate())
        .param("returnRate", value.returnRate())
        .param("drawdown", value.maxDrawdown())
        .param("runup", value.maxRunup())
        .param("volatility", value.volatility())
        .param("outcomeClass", value.outcomeClass())
        .param("status", value.complete() ? "COMPLETE" : "NOT_AVAILABLE")
        .param("evaluationVersion", EVALUATION_VERSION)
        .param("dataVersion", DATA_VERSION)
        .update();
  }

  private record CaseRow(Long caseId, LocalDate baseDate, Long legacyId, Long runId) {}

  private record Price(LocalDate date, BigDecimal value) {}

  private record Evaluation(
      boolean complete,
      BigDecimal basePrice,
      BigDecimal endPrice,
      LocalDate endDate,
      BigDecimal returnRate,
      BigDecimal maxDrawdown,
      BigDecimal maxRunup,
      BigDecimal volatility,
      String outcomeClass) {
    private static Evaluation notAvailable() {
      return new Evaluation(false, null, null, null, null, null, null, null, null);
    }
  }

  public record BatchResult(int cases, int requestedEvaluations, int complete, int notAvailable) {}
}
