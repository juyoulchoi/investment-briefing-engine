package com.nanum.investment.api.v1;

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
            List<AccountSummary> accountSummaries,
            List<ActionSignal> actionSignals,
            List<BriefingArticle> briefingArticles) {}

    public record AccountSummary(
            String accountType, BigDecimal totalAsset, BigDecimal evaluationAmount,
            BigDecimal costAmount, BigDecimal cashAmount, long holdingCount,
            LocalDate priceBaseDate) {}
    public record ActionSignal(
            String stockCode, String stockName, String actionSignal,
            BigDecimal recommendedAmount, String reason) {}

    public record BriefingArticle(String itemCode, String summary, String content, String signalCode) {}

    @GetMapping
    public ApiResponse<DashboardResponse> latest(HttpServletRequest request) {
        DashboardResponse response = jdbc.sql("""
                SELECT d."BASE_DT",d."MKT_SCR",d."MKT_REGIME",d."SENT_SCR",d."SENT_PHASE",
                       d."RISK_GRADE",d."OVR_DEC_SIG",d."REG_BUY_TOT_AMT",d."ADD_BUY_TOT_AMT",
                       b."TITLE",b."SUMMARY_TXT"
                  FROM "TB_INV_DEC" d
                  LEFT JOIN "TB_BRF" b ON b."BASE_DT"=d."BASE_DT" AND b."BRF_TP"='DAILY'
                       AND b."SCOPE_TP"='GLOBAL' AND b."LATEST_YN"='Y'
                 WHERE d."LATEST_YN"='Y'
                 ORDER BY d."BASE_DT" DESC,d."CALC_SEQ" DESC
                 LIMIT 1
                """).query((rs, rowNum) -> {
                    LocalDate baseDate = rs.getObject("BASE_DT", LocalDate.class);
                    return new DashboardResponse(
                            baseDate, rs.getBigDecimal("MKT_SCR"), rs.getString("MKT_REGIME"),
                            rs.getBigDecimal("SENT_SCR"), rs.getString("SENT_PHASE"),
                            rs.getString("RISK_GRADE"), rs.getString("OVR_DEC_SIG"),
                            rs.getBigDecimal("REG_BUY_TOT_AMT"), rs.getBigDecimal("ADD_BUY_TOT_AMT"),
                            rs.getString("TITLE"), rs.getString("SUMMARY_TXT"),
                            accounts(), actions(baseDate), articles(baseDate));
                }).optional().orElse(null);
        return ApiResponse.success(response, TraceIdUtils.resolve(request));
    }

    private List<AccountSummary> accounts() {
        return jdbc.sql("""
                SELECT a."ACCT_TP",
                       COALESCE(sum(h."EVL_AMT"),0)+a."CASH_AMT"+a."RSV_CASH_AMT" AS total_asset,
                       COALESCE(sum(h."EVL_AMT"),0) AS evaluation_amount,
                       COALESCE(sum(h."AVG_PRC"*h."HOLD_QTY"*h."EXCH_RT"),0) AS cost_amount,
                       a."CASH_AMT"+a."RSV_CASH_AMT" AS cash_amount,
                       count(h."HOLD_ID") AS holding_count,max(h."PRC_BASE_DT") AS price_base_date
                  FROM "TB_ACCT" a
                  LEFT JOIN "TB_HOLD" h ON h."ACCT_ID"=a."ACCT_ID"
                       AND h."USE_YN"='Y' AND h."DEL_YN"='N'
                 WHERE a."USE_YN"='Y' AND a."DEL_YN"='N'
                 GROUP BY a."ACCT_ID",a."ACCT_TP",a."DISP_SEQ",a."CASH_AMT",a."RSV_CASH_AMT"
                 ORDER BY a."DISP_SEQ"
                """).query((rs, rowNum) -> new AccountSummary(
                        rs.getString("ACCT_TP"), rs.getBigDecimal("total_asset"),
                        rs.getBigDecimal("evaluation_amount"), rs.getBigDecimal("cost_amount"),
                        rs.getBigDecimal("cash_amount"), rs.getLong("holding_count"),
                        rs.getObject("price_base_date", LocalDate.class))).list();
    }
    private List<ActionSignal> actions(LocalDate baseDate) {
        return jdbc.sql("""
                SELECT s."STK_CD",s."STK_NM",x."ACT_SIG",x."REG_BUY_AMT",x."DEC_RSN"
                  FROM "TB_STK_DEC" x
                  JOIN "TB_STK" s ON s."STK_ID"=x."STK_ID"
                  JOIN "TB_INV_DEC" d ON d."INV_DEC_ID"=x."INV_DEC_ID"
                 WHERE d."BASE_DT"=:day AND d."LATEST_YN"='Y' AND x."ACT_SIG"<>'HOLD'
                 ORDER BY x."REG_BUY_AMT" DESC NULLS LAST,s."STK_CD"
                 LIMIT 3
                """).param("day", baseDate).query((rs, rowNum) -> new ActionSignal(
                        rs.getString("STK_CD"), rs.getString("STK_NM"), rs.getString("ACT_SIG"),
                        rs.getBigDecimal("REG_BUY_AMT"), rs.getString("DEC_RSN"))).list();
    }

    private List<BriefingArticle> articles(LocalDate baseDate) {
        return jdbc.sql("""
                SELECT x."ITEM_CD",x."ITEM_SUM",x."ITEM_CONT",x."SIG_CD"
                  FROM "TB_BRF_DTL" x
                  JOIN "TB_BRF" b ON b."BRF_ID"=x."BRF_ID"
                 WHERE b."BASE_DT"=:day AND b."LATEST_YN"='Y' AND b."PUBL_YN"='Y'
                   AND x."ITEM_CD" IN ('US_STOCK_MKT','KR_STOCK_MKT','MKT_PHASE')
                 ORDER BY array_position(ARRAY['US_STOCK_MKT','KR_STOCK_MKT','MKT_PHASE'],x."ITEM_CD")
                """).param("day", baseDate).query((rs, rowNum) -> new BriefingArticle(
                        rs.getString("ITEM_CD"), rs.getString("ITEM_SUM"),
                        rs.getString("ITEM_CONT"), rs.getString("SIG_CD"))).list();
    }
}
