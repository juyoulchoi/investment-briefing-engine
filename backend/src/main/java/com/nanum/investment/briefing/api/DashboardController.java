package com.nanum.investment.briefing.api;

import com.nanum.investment.briefing.domain.ActionSignal;
import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  private final JdbcClient jdbc;

  public DashboardController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public record DashboardResponse(
      LocalDate baseDate,
      LocalDate briefingBaseDate,
      BigDecimal marketScore,
      String marketRegime,
      BigDecimal sentimentScore,
      String sentimentPhase,
      String riskGrade,
      String overallSignal,
      BigDecimal regularBuyTotal,
      BigDecimal additionalBuyTotal,
      String title,
      String summary,
      String body,
      List<AccountSummary> accountSummaries,
      List<ActionSignal> actionSignals,
      List<BriefingArticle> briefingArticles) {}

  public record AccountSummary(
      String accountType,
      BigDecimal totalAsset,
      BigDecimal evaluationAmount,
      BigDecimal costAmount,
      BigDecimal cashAmount,
      long holdingCount,
      LocalDate priceBaseDate,
      String currencyCode,
      BigDecimal displayTotalAsset,
      BigDecimal displayEvaluationAmount,
      BigDecimal displayCostAmount,
      BigDecimal displayCashAmount) {}

  public record ActionSignal(
      String accountType,
      String stockCode,
      String stockName,
      String actionSignal,
      BigDecimal recommendedAmount,
      String reason) {}

  public record BriefingArticle(
      String itemCode, String summary, String content, String signalCode) {}

  @GetMapping
  public ApiResponse<DashboardResponse> latest(HttpServletRequest request) {
    DashboardResponse response =
        jdbc.sql(
                """
                SELECT d."BASE_DT" AS "INV_BASE_DT",d."MKT_SCR",d."MKT_REGIME",d."SENT_SCR",d."SENT_PHASE",
                       d."RISK_GRADE",d."OVR_DEC_SIG",d."REG_BUY_TOT_AMT",d."ADD_BUY_TOT_AMT",
                       b."BRF_ID",b."BASE_DT" AS "BRF_BASE_DT",b."TITLE",b."SUMMARY_TXT",b."BODY_TXT"
                  FROM "TB_INV_DEC" d
                  LEFT JOIN LATERAL (
                       SELECT x."BRF_ID",x."BASE_DT",x."TITLE",x."SUMMARY_TXT",x."BODY_TXT"
                         FROM "TB_BRF" x
                        WHERE x."PUBL_YN"='Y'
                          AND x."BRF_STS"='PUBLISHED'
                          AND x."BRF_TP"='DAILY'
                          AND x."SCOPE_TP"='GLOBAL'
                          AND x."LATEST_YN"='Y'
                        ORDER BY x."BASE_DT" DESC,x."BRF_ID" DESC
                        LIMIT 1
                  ) b ON TRUE
                 WHERE d."LATEST_YN"='Y'
                 ORDER BY d."BASE_DT" DESC,d."CALC_SEQ" DESC
                 LIMIT 1
                """)
            .query(
                (rs, rowNum) -> {
                  LocalDate baseDate = rs.getObject("INV_BASE_DT", LocalDate.class);
                  Long briefingId = rs.getObject("BRF_ID", Long.class);
                  return new DashboardResponse(
                      baseDate,
                      rs.getObject("BRF_BASE_DT", LocalDate.class),
                      rs.getBigDecimal("MKT_SCR"),
                      rs.getString("MKT_REGIME"),
                      rs.getBigDecimal("SENT_SCR"),
                      rs.getString("SENT_PHASE"),
                      rs.getString("RISK_GRADE"),
                      rs.getString("OVR_DEC_SIG"),
                      rs.getBigDecimal("REG_BUY_TOT_AMT"),
                      rs.getBigDecimal("ADD_BUY_TOT_AMT"),
                      rs.getString("TITLE"),
                      rs.getString("SUMMARY_TXT"),
                      rs.getString("BODY_TXT"),
                      accounts(),
                      actions(baseDate),
                      articles(briefingId));
                })
            .optional()
            .orElse(null);
    return ApiResponse.success(response, TraceIdUtils.resolve(request));
  }

  private List<AccountSummary> accounts() {
    return jdbc.sql(
            """
                SELECT a."ACCT_TP",
                       COALESCE(sum(h."EVL_AMT"),0)+a."CASH_AMT"+a."RSV_CASH_AMT" AS total_asset,
                       COALESCE(sum(h."EVL_AMT"),0) AS evaluation_amount,
                       COALESCE(sum(h."AVG_PRC"*h."HOLD_QTY"*h."EXCH_RT"),0) AS cost_amount,
                       a."CASH_AMT"+a."RSV_CASH_AMT" AS cash_amount,
                       count(h."HOLD_ID") AS holding_count,max(h."PRC_BASE_DT") AS price_base_date,
                       CASE WHEN a."ACCT_TP"='OVERSEAS' THEN 'USD' ELSE 'KRW' END AS currency_code,
                       CASE WHEN a."ACCT_TP"='OVERSEAS'
                            THEN COALESCE(sum(h."ORG_EVL_AMT"),0)
                            ELSE COALESCE(sum(h."EVL_AMT"),0)+a."CASH_AMT"+a."RSV_CASH_AMT" END AS display_total_asset,
                       CASE WHEN a."ACCT_TP"='OVERSEAS' THEN COALESCE(sum(h."ORG_EVL_AMT"),0)
                            ELSE COALESCE(sum(h."EVL_AMT"),0) END AS display_evaluation_amount,
                       CASE WHEN a."ACCT_TP"='OVERSEAS' THEN COALESCE(sum(h."AVG_PRC"*h."HOLD_QTY"),0)
                            ELSE COALESCE(sum(h."AVG_PRC"*h."HOLD_QTY"*h."EXCH_RT"),0) END AS display_cost_amount,
                       a."CASH_AMT"+a."RSV_CASH_AMT" AS display_cash_amount
                  FROM "TB_ACCT" a
                  LEFT JOIN "TB_HOLD" h ON h."ACCT_ID"=a."ACCT_ID"
                       AND h."USE_YN"='Y' AND h."DEL_YN"='N'
                 WHERE a."DEL_YN"='N'
                 GROUP BY a."ACCT_ID",a."ACCT_TP",a."DISP_SEQ",a."CASH_AMT",a."RSV_CASH_AMT",a."BASE_CURR_CD"
                 ORDER BY a."DISP_SEQ"
                """)
        .query(
            (rs, rowNum) ->
                new AccountSummary(
                    rs.getString("ACCT_TP"), rs.getBigDecimal("total_asset"),
                    rs.getBigDecimal("evaluation_amount"), rs.getBigDecimal("cost_amount"),
                    rs.getBigDecimal("cash_amount"), rs.getLong("holding_count"),
                    rs.getObject("price_base_date", LocalDate.class), rs.getString("currency_code"),
                    rs.getBigDecimal("display_total_asset"),
                        rs.getBigDecimal("display_evaluation_amount"),
                    rs.getBigDecimal("display_cost_amount"),
                        rs.getBigDecimal("display_cash_amount")))
        .list();
  }

  private List<ActionSignal> actions(LocalDate baseDate) {
    return jdbc.sql(
            """
                SELECT a."ACCT_TP",s."STK_CD",s."STK_NM",x."ACT_SIG",x."REG_BUY_AMT",x."DEC_RSN"
                  FROM "TB_STK_DEC" x
                  JOIN "TB_STK" s ON s."STK_ID"=x."STK_ID"
                  JOIN "TB_ACCT" a ON a."ACCT_ID"=x."ACCT_ID"
                  JOIN "TB_INV_DEC" d ON d."INV_DEC_ID"=x."INV_DEC_ID"
                 WHERE d."BASE_DT"=:day AND d."LATEST_YN"='Y' AND x."ACT_SIG"<>'HOLD'
                 ORDER BY x."REG_BUY_AMT" DESC NULLS LAST,s."STK_CD"
                 LIMIT 3
                """)
        .param("day", baseDate)
        .query(
            (rs, rowNum) ->
                new ActionSignal(
                    rs.getString("ACCT_TP"),
                    rs.getString("STK_CD"),
                    rs.getString("STK_NM"),
                    rs.getString("ACT_SIG"),
                    rs.getBigDecimal("REG_BUY_AMT"),
                    rs.getString("DEC_RSN")))
        .list();
  }

  private List<BriefingArticle> articles(Long briefingId) {
    if (briefingId == null) return List.of();
    return jdbc.sql(
            """
                SELECT x."ITEM_CD",x."ITEM_SUM",x."ITEM_CONT",x."SIG_CD"
                  FROM "TB_BRF_DTL" x
                 WHERE x."BRF_ID"=:briefingId
                 ORDER BY x."DTL_ID"
                """)
        .param("briefingId", briefingId)
        .query(
            (rs, rowNum) ->
                new BriefingArticle(
                    rs.getString("ITEM_CD"), rs.getString("ITEM_SUM"),
                    rs.getString("ITEM_CONT"), rs.getString("SIG_CD")))
        .list();
  }
}
