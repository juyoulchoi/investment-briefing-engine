package com.nanum.investment.briefing.api;

import com.nanum.investment.briefing.application.MarketDirectionPredictionService;
import com.nanum.investment.briefing.dto.request.MarketDirectionDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/market-direction")
public class MarketDirectionPredictionAdminController {
  private final JdbcClient jdbc;
  private final MarketDirectionPredictionService predictions;

  public MarketDirectionPredictionAdminController(
      JdbcClient jdbc, MarketDirectionPredictionService predictions) {
    this.jdbc = jdbc;
    this.predictions = predictions;
  }

  @org.springframework.web.bind.annotation.PostMapping("/predictions/calculate")
  public MarketDirectionDto calculate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return predictions.calculateAndSave(baseDate);
  }

  @GetMapping("/predictions")
  public List<Map<String, Object>> predictions(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate baseDate) {
    return jdbc.sql(
            """
        SELECT "MKT_DIR_PRED_ID" AS "predictionId", "BASE_DT" AS "baseDate",
               "CALC_SEQ" AS "calculationSequence", "MODEL_VER_CD" AS "modelVersion",
               "PRED_PERIOD_TP" AS "predictionPeriod", "DIR_SCR" AS "directionScore",
               "CONF_SCR" AS "confidenceScore", "CONF_GRADE" AS "confidenceGrade",
               "MODEL_STATUS" AS "modelStatus", "LATEST_YN" AS "latestYn",
               "SELECTED_YN" AS "selectedYn", "FALLBACK_RSN" AS "fallbackReason"
        FROM "TB_MKT_DIR_PRED"
        WHERE (CAST(:baseDate AS date) IS NULL OR "BASE_DT"=:baseDate)
        ORDER BY "BASE_DT" DESC, "MODEL_VER_CD", "CALC_SEQ" DESC
        LIMIT 200
        """)
        .param("baseDate", baseDate)
        .query(
            (rs, n) -> {
              Map<String, Object> row = new java.util.LinkedHashMap<>();
              row.put("predictionId", rs.getLong("predictionId"));
              row.put("baseDate", rs.getObject("baseDate", LocalDate.class));
              row.put("calculationSequence", rs.getInt("calculationSequence"));
              row.put("modelVersion", rs.getString("modelVersion"));
              row.put("predictionPeriod", rs.getString("predictionPeriod"));
              row.put("directionScore", rs.getInt("directionScore"));
              row.put("confidenceScore", rs.getBigDecimal("confidenceScore"));
              row.put("confidenceGrade", rs.getString("confidenceGrade"));
              row.put("modelStatus", rs.getString("modelStatus"));
              row.put("latestYn", rs.getString("latestYn"));
              row.put("selectedYn", rs.getString("selectedYn"));
              row.put("fallbackReason", rs.getString("fallbackReason"));
              return row;
            })
        .list();
  }
}
