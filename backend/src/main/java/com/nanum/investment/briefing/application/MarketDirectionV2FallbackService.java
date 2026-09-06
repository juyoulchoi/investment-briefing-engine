package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.briefing.dto.request.MarketDirectionDto;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class MarketDirectionV2FallbackService {
  static final int REQUIRED_SAMPLE_COUNT = 252;
  private static final List<FactorDefinition> FACTORS =
      List.of(
          new FactorDefinition("FOREIGN_FLOW", 20),
          new FactorDefinition("FX_DOLLAR", 20),
          new FactorDefinition("US_RATE", 15),
          new FactorDefinition("SEMICONDUCTOR", 15),
          new FactorDefinition("MARKET_INTERNAL", 15),
          new FactorDefinition("PRICE_TREND", 15));

  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public MarketDirectionV2FallbackService(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public void saveFallback(LocalDate date, MarketDirectionDto v1) {
    int exchangeSamples = sampleCount("TB_EXCH_DAY", "BASE_DT", date);
    int snapshotSamples = sampleCount("TB_MKT_SNAP", "BASE_DT", date);
    String reason =
        "V2 핵심 Factor 계산 이력 부족: 환율 "
            + exchangeSamples
            + "/"
            + REQUIRED_SAMPLE_COUNT
            + ", 시장 Snapshot "
            + snapshotSamples
            + "/"
            + REQUIRED_SAMPLE_COUNT
            + ", 외국인 수급 원천 미확정";

    jdbc.sql(
            "UPDATE \"TB_MKT_DIR_PRED\" SET \"LATEST_YN\"='N' WHERE \"BASE_DT\"=:day AND \"MODEL_VER_CD\"='V2.0' AND \"PRED_PERIOD_TP\"='DAILY' AND \"LATEST_YN\"='Y'")
        .param("day", date)
        .update();
    int sequence =
        jdbc.sql(
                "SELECT COALESCE(max(\"CALC_SEQ\"),0)+1 FROM \"TB_MKT_DIR_PRED\" WHERE \"BASE_DT\"=:day")
            .param("day", date)
            .query(Integer.class)
            .single();
    Map<String, Object> basis = new LinkedHashMap<>();
    basis.put("status", "INSUFFICIENT");
    basis.put("requiredSamples", REQUIRED_SAMPLE_COUNT);
    basis.put("exchangeSamples", exchangeSamples);
    basis.put("marketSnapshotSamples", snapshotSamples);
    basis.put("selectedModel", "V1");
    basis.put("reason", reason);

    long predictionId =
        jdbc.sql(
                """
        INSERT INTO "TB_MKT_DIR_PRED"(
          "BASE_DT","CALC_SEQ","DIR_SCR","UPTREND_RESUME_PROB","BOX_RANGE_PROB",
          "RE_CORRECTION_PROB","RETEST_LOW_PROB","UPTREND_RESUME_CHG","BOX_RANGE_CHG",
          "RE_CORRECTION_CHG","RETEST_LOW_CHG","INPUT_BASE_DT_JSON","CALC_BASIS_JSON",
          "RULE_VER_NO","LATEST_YN","MODEL_VER_CD","PRED_PERIOD_TP","BASE_DIR_SCR",
          "RAW_INTERACTION_ADJ_SCR","INTERACTION_ADJ_SCR","CONF_SCR","CONF_GRADE",
          "MODEL_STATUS","SELECTED_YN","FALLBACK_RSN")
        VALUES(:day,:seq,:score,:up,:box,:correction,:low,0,0,0,0,CAST(:inputs AS jsonb),
          CAST(:basis AS jsonb),2,'Y','V2.0','DAILY',:score,0,0,0,'INSUFFICIENT',
          'FALLBACK','N',:reason)
        RETURNING "MKT_DIR_PRED_ID"
        """)
            .param("day", date)
            .param("seq", sequence)
            .param("score", v1.score())
            .param("up", v1.scenarios().uptrendResume())
            .param("box", v1.scenarios().boxRange())
            .param("correction", v1.scenarios().reCorrection())
            .param("low", v1.scenarios().retestLow())
            .param("inputs", toJson(Map.of("sourceModel", "V1", "baseDate", date)))
            .param("basis", toJson(basis))
            .param("reason", reason)
            .query(Long.class)
            .single();

    for (FactorDefinition factor : FACTORS) {
      int samples =
          switch (factor.code()) {
            case "FX_DOLLAR" -> exchangeSamples;
            case "PRICE_TREND", "MARKET_INTERNAL" -> snapshotSamples;
            default -> 0;
          };
      String status = samples >= REQUIRED_SAMPLE_COUNT ? "MISSING" : "INSUFFICIENT_HISTORY";
      jdbc.sql(
              """
          INSERT INTO "TB_MKT_DIR_PRED_FCTR"(
            "MKT_DIR_PRED_ID","FACTOR_CD","FACTOR_WEIGHT","FACTOR_CONFIDENCE",
            "AVAILABLE_WEIGHT","FACTOR_STATUS","STATUS_RSN")
          VALUES(:id,:code,:weight,0,0,:status,:reason)
          """)
          .param("id", predictionId)
          .param("code", factor.code())
          .param("weight", factor.weight())
          .param("status", status)
          .param(
              "reason",
              samples == 0
                  ? "검증된 원천 데이터 매핑이 아직 없습니다."
                  : samples < REQUIRED_SAMPLE_COUNT
                      ? "표본 " + samples + "건으로 최소 " + REQUIRED_SAMPLE_COUNT + "건 미만입니다."
                      : "V2 Metric 계산기가 아직 구현되지 않았습니다.")
          .update();
    }
  }

  static boolean hasSufficientHistory(int sampleCount) {
    return sampleCount >= REQUIRED_SAMPLE_COUNT;
  }

  private int sampleCount(String table, String dateColumn, LocalDate date) {
    String sql =
        "SELECT count(DISTINCT \""
            + dateColumn
            + "\") FROM \""
            + table
            + "\" WHERE \""
            + dateColumn
            + "\"<:day";
    return jdbc.sql(sql).param("day", date).query(Integer.class).single();
  }

  private String toJson(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("V2 계산 근거를 JSON으로 변환할 수 없습니다.", e);
    }
  }

  private record FactorDefinition(String code, int weight) {}
}
