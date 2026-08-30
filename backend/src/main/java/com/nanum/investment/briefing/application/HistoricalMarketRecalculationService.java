package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.briefing.domain.calculation.MarketRegimeClassifier;
import com.nanum.investment.briefing.domain.calculation.MarketRegimeInput;
import com.nanum.investment.briefing.domain.calculation.MarketRuleSet;
import com.nanum.investment.briefing.domain.calculation.MarketScoreCalculator;
import com.nanum.investment.briefing.domain.calculation.MarketScoreInput;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricalMarketRecalculationService {
  private static final List<String> REQUIRED_KRX =
      List.of("KOSPI_INDEX_DAILY", "KOSPI_STOCK_DAILY", "KOSDAQ_STOCK_DAILY", "ETF_DAILY");
  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;
  private final MarketScoreCalculator scoreCalculator;
  private final MarketRegimeClassifier regimeClassifier;

  public HistoricalMarketRecalculationService(
      JdbcClient jdbc,
      ObjectMapper objectMapper,
      MarketScoreCalculator scoreCalculator,
      MarketRegimeClassifier regimeClassifier) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
    this.scoreCalculator = scoreCalculator;
    this.regimeClassifier = regimeClassifier;
  }

  @Transactional
  public RecalculationResult recalculate(
      LocalDate baseDate, String ruleVersion, String codeVersion) {
    if (baseDate == null) throw new IllegalArgumentException("과거 재계산 기준일이 필요합니다.");
    if (baseDate.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("미래 날짜는 재계산할 수 없습니다.");
    String normalizedRule =
        ruleVersion == null || ruleVersion.isBlank() ? "MARKET_RULE_V1" : ruleVersion.trim();
    Long legacyId = findLegacyId(baseDate);
    Long runId = createRun(legacyId, baseDate, normalizedRule, codeVersion);
    try {
      HistoricalInputs inputs = loadInputs(baseDate);
      List<String> missing = missing(inputs);
      int present = 10 - missing.size();
      BigDecimal confidence = BigDecimal.valueOf(present * 10L);
      MarketScoreInput scoreInput =
          new MarketScoreInput(
              inputs.kospiChange(),
              inputs.kosdaqChange(),
              inputs.foreignNetAmount(),
              inputs.institutionNetAmount(),
              inputs.foreignFuturesNetQuantity(),
              inputs.programNetAmount(),
              inputs.exchangeChange(),
              inputs.vixValue(),
              inputs.vixChange(),
              inputs.breadthRate(),
              null,
              inputs.liquidityScore(),
              inputs.usTwoYearYield(),
              inputs.usTenYearYield(),
              confidence);
      var score = scoreCalculator.calculate(scoreInput, MarketRuleSet.defaultRules());
      var regime =
          regimeClassifier.classify(
              new MarketRegimeInput(
                  score.totalScore(),
                  inputs.kospiChange(),
                  inputs.breadthRate(),
                  inputs.vixValue(),
                  inputs.vixChange(),
                  inputs.exchangeChange(),
                  inputs.foreignNetAmount(),
                  inputs.foreignFuturesNetQuantity(),
                  BigDecimal.ZERO,
                  false),
              MarketRuleSet.defaultRules());
      BigDecimal riskScore =
          BigDecimal.valueOf(100).subtract(score.totalScore()).setScale(4, RoundingMode.HALF_UP);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("marketHealthScore", score.totalScore());
      result.put("marketRiskScore", riskScore);
      result.put("marketRegime", regime.value().name());
      result.put("dataConfidenceRate", confidence);
      result.put("components", componentMap(score));
      result.put("ruleVersion", normalizedRule);
      result.put("portfolioMode", "MARKET_ONLY");
      String dataStatus = missing.isEmpty() ? "COMPLETE" : present >= 5 ? "PARTIAL" : "FAILED";
      finish(runId, inputs, missing, result, dataStatus, "SUCCESS", null);
      return new RecalculationResult(runId, legacyId, baseDate, dataStatus, missing, result);
    } catch (RuntimeException exception) {
      fail(runId, exception.getMessage());
      throw exception;
    }
  }

  public List<ComparisonRow> comparisons(LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            SELECT l."LEGACY_BRF_ID",l."MARKET_BASE_DT",e."MKT_RISK_SCR",e."MKT_PHASE_CD",e."REG_BUY_SIG_CD",
                   r."HIST_RECALC_RUN_ID",r."DATA_STS",r."RESULT_JSON"->>'marketRiskScore',
                   r."RESULT_JSON"->>'marketRegime',r."RULE_VER",r."RECONSTRUCTION_MODE"
              FROM "TB_LEGACY_BRF" l
              LEFT JOIN "TB_LEGACY_BRF_EXTRACT" e ON e."LEGACY_BRF_ID"=l."LEGACY_BRF_ID" AND e."EXTRACT_VER"=1
              LEFT JOIN LATERAL (
                SELECT x.* FROM "TB_HIST_RECALC_RUN" x
                 WHERE x."BASE_DT"=l."MARKET_BASE_DT" AND x."RUN_STS"='SUCCESS'
                 ORDER BY x."START_DTTM" DESC LIMIT 1
              ) r ON true
             WHERE l."MARKET_BASE_DT" BETWEEN :from AND :to
             ORDER BY l."MARKET_BASE_DT"
            """)
        .param("from", from)
        .param("to", to)
        .query(
            (rs, n) ->
                new ComparisonRow(
                    rs.getLong(1),
                    rs.getObject(2, LocalDate.class),
                    (Integer) rs.getObject(3),
                    rs.getString(4),
                    rs.getString(5),
                    (Long) rs.getObject(6),
                    rs.getString(7),
                    rs.getString(8) == null ? null : new BigDecimal(rs.getString(8)),
                    rs.getString(9),
                    rs.getString(10),
                    rs.getString(11)))
        .list();
  }

  private HistoricalInputs loadInputs(LocalDate day) {
    Map<String, DatedValue> indices = new LinkedHashMap<>();
    for (String code : List.of("KOSPI", "KOSDAQ", "VIX")) indices.put(code, index(code, day));
    DatedValue exchange =
        dated(
            "SELECT \"BASE_DT\",\"CHG_RT\" FROM \"TB_EXCH_DAY\" WHERE \"BASE_CURR_CD\"='USD' AND \"QUOTE_CURR_CD\"='KRW' AND \"BASE_DT\"<=:day ORDER BY \"BASE_DT\" DESC LIMIT 1",
            day);
    DatedValue dgs2 = bond("DGS2", day), dgs10 = bond("DGS10", day);
    SnapshotValues snapshot = snapshot(day);
    Map<String, LocalDate> dates = new LinkedHashMap<>();
    indices.forEach((key, value) -> dates.put(key, value.date()));
    dates.put("USD_KRW", exchange.date());
    dates.put("DGS2", dgs2.date());
    dates.put("DGS10", dgs10.date());
    dates.put("KR_MARKET_SNAPSHOT", snapshot.date());
    Map<String, Long> availability = new LinkedHashMap<>();
    for (String dataset : REQUIRED_KRX) availability.put(dataset, count(dataset, day));
    availability.put("USD_KRW", exchange.value() == null ? 0L : 1L);
    availability.put("DGS2", dgs2.value() == null ? 0L : 1L);
    availability.put("DGS10", dgs10.value() == null ? 0L : 1L);
    availability.put("VIX", indices.get("VIX").value() == null ? 0L : 1L);
    return new HistoricalInputs(
        indices.get("KOSPI").value(),
        indices.get("KOSDAQ").value(),
        snapshot.foreignNet(),
        snapshot.institutionNet(),
        snapshot.foreignFuturesNet(),
        snapshot.programNet(),
        exchange.value(),
        indices.get("VIX").level(),
        indices.get("VIX").value(),
        snapshot.breadth(),
        snapshot.liquidity(),
        dgs2.level(),
        dgs10.level(),
        dates,
        availability);
  }

  private DatedValue index(String code, LocalDate day) {
    if ("KOSPI".equals(code) || "KOSDAQ".equals(code)) {
      return jdbc.sql(
              """
              SELECT base_date,NULLIF(payload->>'FLUC_RT','')::numeric,
                     NULLIF(replace(payload->>'CLSPRC_IDX',',',''),'')::numeric
                FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date<=:day
                 AND payload->>'IDX_NM' IN (:name,:code) ORDER BY base_date DESC LIMIT 1
              """)
          .param("dataset", code + "_INDEX_DAILY")
          .param("name", "KOSPI".equals(code) ? "코스피" : "코스닥")
          .param("code", code)
          .param("day", day)
          .query(
              (rs, n) ->
                  new DatedValue(
                      rs.getObject(1, LocalDate.class), rs.getBigDecimal(2), rs.getBigDecimal(3)))
          .optional()
          .orElse(new DatedValue(null, null, null));
    }
    return jdbc.sql(
            "SELECT \"TRADE_DT\",\"CHG_RT\",\"CLS_VAL\" FROM \"TB_IDX_DAY\" WHERE \"IDX_CD\"=:code AND \"TRADE_DT\"<=:day ORDER BY \"TRADE_DT\" DESC LIMIT 1")
        .param("code", code)
        .param("day", day)
        .query(
            (rs, n) ->
                new DatedValue(
                    rs.getObject(1, LocalDate.class), rs.getBigDecimal(2), rs.getBigDecimal(3)))
        .optional()
        .orElse(new DatedValue(null, null, null));
  }

  private DatedValue dated(String sql, LocalDate day) {
    return jdbc.sql(sql)
        .param("day", day)
        .query(
            (rs, n) -> new DatedValue(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2), null))
        .optional()
        .orElse(new DatedValue(null, null, null));
  }

  private DatedValue bond(String code, LocalDate day) {
    return jdbc.sql(
            "SELECT \"BASE_DT\",\"CHG_BP\",\"YLD_RT\" FROM \"TB_FRED_BOND_DAY\" WHERE \"BOND_CD\"=:code AND \"BASE_DT\"<=:day ORDER BY \"BASE_DT\" DESC LIMIT 1")
        .param("code", code)
        .param("day", day)
        .query(
            (rs, n) ->
                new DatedValue(
                    rs.getObject(1, LocalDate.class), rs.getBigDecimal(2), rs.getBigDecimal(3)))
        .optional()
        .orElse(new DatedValue(null, null, null));
  }

  private SnapshotValues snapshot(LocalDate day) {
    return jdbc.sql(
            """
            SELECT "BASE_DT","FRGN_NET_AMT","INST_NET_AMT","FRGN_FUT_NET_QTY","PGM_NET_AMT","MKT_BREADTH_RT","LIQD_SCR"
              FROM "TB_MKT_SNAP" WHERE "BASE_DT"=:day AND "MKT_SNAP_CD"='KR_MARKET'
            """)
        .param("day", day)
        .query(
            (rs, n) ->
                new SnapshotValues(
                    rs.getObject(1, LocalDate.class),
                    rs.getBigDecimal(2),
                    rs.getBigDecimal(3),
                    rs.getBigDecimal(4),
                    rs.getBigDecimal(5),
                    rs.getBigDecimal(6),
                    rs.getBigDecimal(7)))
        .optional()
        .orElse(new SnapshotValues(null, null, null, null, null, null, null));
  }

  private long count(String dataset, LocalDate day) {
    return jdbc.sql(
            "SELECT count(*) FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date=:day")
        .param("dataset", dataset)
        .param("day", day)
        .query(Long.class)
        .single();
  }

  private List<String> missing(HistoricalInputs value) {
    List<String> missing = new ArrayList<>();
    if (value.kospiChange() == null) missing.add("KOSPI");
    if (value.kosdaqChange() == null) missing.add("KOSDAQ");
    if (value.foreignNetAmount() == null) missing.add("FOREIGN_NET_AMOUNT");
    if (value.institutionNetAmount() == null) missing.add("INSTITUTION_NET_AMOUNT");
    if (value.foreignFuturesNetQuantity() == null) missing.add("FOREIGN_FUTURES_FLOW");
    if (value.programNetAmount() == null) missing.add("PROGRAM_TRADING");
    if (value.exchangeChange() == null) missing.add("USD_KRW");
    if (value.vixValue() == null) missing.add("VIX");
    if (value.usTwoYearYield() == null) missing.add("DGS2");
    if (value.usTenYearYield() == null) missing.add("DGS10");
    return List.copyOf(missing);
  }

  private Map<String, Object> componentMap(
      com.nanum.investment.briefing.domain.calculation.MarketScoreResult score) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("trend", score.trendScore());
    values.put("flow", score.flowScore());
    values.put("futuresProgram", score.futuresProgramScore());
    values.put("breadth", score.breadthScore());
    values.put("volatility", score.volatilityScore());
    values.put("exchange", score.exchangeScore());
    values.put("rate", score.rateScore());
    values.put("liquidity", score.liquidityScore());
    return values;
  }

  private Long findLegacyId(LocalDate baseDate) {
    return jdbc.sql(
            "SELECT \"LEGACY_BRF_ID\" FROM \"TB_LEGACY_BRF\" WHERE \"MARKET_BASE_DT\"=:day ORDER BY \"GENERATED_DTTM\" LIMIT 1")
        .param("day", baseDate)
        .query(Long.class)
        .optional()
        .orElse(null);
  }

  private Long createRun(
      Long legacyId, LocalDate baseDate, String ruleVersion, String codeVersion) {
    Long caseId =
        jdbc.sql(
                """
                INSERT INTO "TB_BRF_CASE"("MKT_BASE_DT","BRF_TP","AS_OF_DTTM","CASE_STS")
                VALUES(:baseDate,'DAILY',(CAST(:baseDate AS date) + TIME '08:00') AT TIME ZONE 'Asia/Seoul','READY')
                ON CONFLICT("MKT_BASE_DT","BRF_TP") DO UPDATE SET "MKT_BASE_DT"=EXCLUDED."MKT_BASE_DT"
                RETURNING "CASE_ID"
                """)
            .param("baseDate", baseDate)
            .query(Long.class)
            .single();
    return jdbc.sql(
            """
            INSERT INTO "TB_HIST_RECALC_RUN"("CASE_ID","LEGACY_BRF_ID","BASE_DT","RULE_VER","CODE_VER","RECONSTRUCTION_MODE",
             "PORTFOLIO_MODE","DATA_STS","RUN_STS")
            VALUES(:caseId,:legacyId,:baseDate,:ruleVersion,:codeVersion,'AS_OF_RECONSTRUCTED','MARKET_ONLY','PARTIAL','RUNNING')
            RETURNING "HIST_RECALC_RUN_ID"
            """)
        .param("legacyId", legacyId)
        .param("caseId", caseId)
        .param("baseDate", baseDate)
        .param("ruleVersion", ruleVersion)
        .param("codeVersion", codeVersion)
        .query(Long.class)
        .single();
  }

  private void finish(
      Long runId,
      HistoricalInputs inputs,
      List<String> missing,
      Map<String, Object> result,
      String dataStatus,
      String runStatus,
      String failure) {
    jdbc.sql(
            """
            UPDATE "TB_HIST_RECALC_RUN" SET "INPUT_BASE_DT_JSON"=CAST(:dates AS jsonb),
             "AVAILABILITY_JSON"=CAST(:availability AS jsonb),"MISSING_INPUT_JSON"=CAST(:missing AS jsonb),
             "RESULT_JSON"=CAST(:result AS jsonb),"DATA_STS"=:dataStatus,"RUN_STS"=:runStatus,
             "FAIL_RSN"=:failure,"END_DTTM"=CURRENT_TIMESTAMP WHERE "HIST_RECALC_RUN_ID"=:id
            """)
        .param("dates", json(inputs.inputDates()))
        .param("availability", json(inputs.availability()))
        .param("missing", json(missing))
        .param("result", json(result))
        .param("dataStatus", dataStatus)
        .param("runStatus", runStatus)
        .param("failure", failure)
        .param("id", runId)
        .update();
    storeInputLineage(runId, inputs, missing);
  }

  private void storeInputLineage(Long runId, HistoricalInputs inputs, List<String> missingInputs) {
    jdbc.sql(
            """
            INSERT INTO "TB_HIST_RECALC_INPUT"("HIST_RECALC_RUN_ID","INPUT_CD","REQUIRED_YN","AVAILABLE_YN","BASE_DT",
             "SOURCE_CD","SOURCE_VER","MISSING_RSN","RECONSTRUCTION_METHOD")
            SELECT :runId,a.key,'Y',CASE WHEN (a.value#>>'{}')::bigint>0 THEN 'Y' ELSE 'N' END,
                   CASE WHEN CAST(:dates AS jsonb) ? a.key THEN (CAST(:dates AS jsonb)->>a.key)::date END,
                   CASE WHEN a.key LIKE 'DGS%' THEN 'FRED' WHEN a.key IN ('USD_KRW','VIX') THEN 'YAHOO' ELSE 'KRX' END,
                   (SELECT "CODE_VER" FROM "TB_HIST_RECALC_RUN" WHERE "HIST_RECALC_RUN_ID"=:runId),
                   CASE WHEN (a.value#>>'{}')::bigint=0 THEN '과거 원천 데이터 없음' END,
                   CASE WHEN (a.value#>>'{}')::bigint>0 THEN 'RAW' ELSE 'NOT_AVAILABLE' END
              FROM jsonb_each(CAST(:availability AS jsonb)) a
            ON CONFLICT("HIST_RECALC_RUN_ID","INPUT_CD") DO NOTHING
            """)
        .param("runId", runId)
        .param("dates", json(inputs.inputDates()))
        .param("availability", json(inputs.availability()))
        .update();
    jdbc.sql(
            """
            INSERT INTO "TB_HIST_RECALC_INPUT"("HIST_RECALC_RUN_ID","INPUT_CD","REQUIRED_YN","AVAILABLE_YN",
             "SOURCE_VER","MISSING_RSN","RECONSTRUCTION_METHOD")
            SELECT :runId,value,'Y','N',
                   (SELECT "CODE_VER" FROM "TB_HIST_RECALC_RUN" WHERE "HIST_RECALC_RUN_ID"=:runId),
                   '재계산 입력 Snapshot에 값이 없음','NOT_AVAILABLE'
              FROM jsonb_array_elements_text(CAST(:missing AS jsonb))
            ON CONFLICT("HIST_RECALC_RUN_ID","INPUT_CD") DO NOTHING
            """)
        .param("runId", runId)
        .param("missing", json(missingInputs))
        .update();
  }

  private void fail(Long runId, String failure) {
    jdbc.sql(
            "UPDATE \"TB_HIST_RECALC_RUN\" SET \"DATA_STS\"='FAILED',\"RUN_STS\"='FAILED',\"FAIL_RSN\"=:failure,\"END_DTTM\"=CURRENT_TIMESTAMP WHERE \"HIST_RECALC_RUN_ID\"=:id")
        .param(
            "failure",
            failure == null ? "재계산 실패" : failure.substring(0, Math.min(2000, failure.length())))
        .param("id", runId)
        .update();
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("재계산 계보 JSON 직렬화에 실패했습니다.", exception);
    }
  }

  private record DatedValue(LocalDate date, BigDecimal value, BigDecimal level) {}

  private record SnapshotValues(
      LocalDate date,
      BigDecimal foreignNet,
      BigDecimal institutionNet,
      BigDecimal foreignFuturesNet,
      BigDecimal programNet,
      BigDecimal breadth,
      BigDecimal liquidity) {}

  private record HistoricalInputs(
      BigDecimal kospiChange,
      BigDecimal kosdaqChange,
      BigDecimal foreignNetAmount,
      BigDecimal institutionNetAmount,
      BigDecimal foreignFuturesNetQuantity,
      BigDecimal programNetAmount,
      BigDecimal exchangeChange,
      BigDecimal vixValue,
      BigDecimal vixChange,
      BigDecimal breadthRate,
      BigDecimal liquidityScore,
      BigDecimal usTwoYearYield,
      BigDecimal usTenYearYield,
      Map<String, LocalDate> inputDates,
      Map<String, Long> availability) {}

  public record RecalculationResult(
      Long runId,
      Long legacyBriefingId,
      LocalDate baseDate,
      String dataStatus,
      List<String> missingInputs,
      Map<String, Object> result) {}

  public record ComparisonRow(
      Long legacyBriefingId,
      LocalDate baseDate,
      Integer legacyRiskScore,
      String legacyMarketPhase,
      String legacyRegularBuySignal,
      Long recalculationRunId,
      String recalculationDataStatus,
      BigDecimal recalculatedRiskScore,
      String recalculatedMarketRegime,
      String ruleVersion,
      String reconstructionMode) {}
}
