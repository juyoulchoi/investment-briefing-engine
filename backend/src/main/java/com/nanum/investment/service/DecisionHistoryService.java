package com.nanum.investment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.api.InvestmentDecisionRequest;
import com.nanum.investment.domain.PortfolioDecision;
import com.nanum.investment.domain.StockDecision;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionHistoryService {
    private final JdbcClient jdbc;
    private final ObjectMapper json;
    public DecisionHistoryService(JdbcClient jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}

    @Transactional
    public void save(InvestmentDecisionRequest request,PortfolioDecision decision){
        jdbc.sql("UPDATE \"TB_INV_DEC\" SET \"LATEST_YN\"='N' WHERE \"BASE_DT\"=:date AND \"ACCT_ID\" IS NULL AND \"MKT_SNAP_CD\"='GLOBAL' AND \"LATEST_YN\"='Y'").param("date",decision.decisionDate()).update();
        Long id=jdbc.sql("""
            INSERT INTO "TB_INV_DEC"(
              "BASE_DT","CALC_SEQ","MKT_SNAP_CD","MKT_SCR","MKT_REGIME","SENT_SCR","SENT_PHASE","RISK_SCR","RISK_GRADE",
              "REG_BUY_BGT_AMT","REG_BUY_TOT_AMT","RSV_ADD_AMT","ADD_BUY_BGT_AMT","OVR_DEC_SIG","CONF_RT","DATA_STS",
              "TOT_MIN_BUY_AMT","TOT_RCM_BUY_AMT","NEW_RSV_CASH","AVAIL_ADD_BUY_CASH","REQ_PAYLOAD","RSLT_PAYLOAD","CRT_USR_ID")
            VALUES(:date,(SELECT COALESCE(MAX("CALC_SEQ"),0)+1 FROM "TB_INV_DEC" WHERE "BASE_DT"=:date AND "ACCT_ID" IS NULL AND "MKT_SNAP_CD"='GLOBAL'),
              'GLOBAL',:marketScore,:regime,:riskScore,:phase,:riskScore,
              CASE WHEN :riskScore>=75 THEN 'CRITICAL' WHEN :riskScore>=50 THEN 'HIGH' WHEN :riskScore>=25 THEN 'MEDIUM' ELSE 'LOW' END,
              :minimum,:recommended,:reserved,:available,
              CASE WHEN :regime='CRASH_RISK' THEN 'PAUSE_BUY' WHEN :regime='STRONG_CORRECTION' THEN 'SELECTIVE_ADD_BUY' ELSE 'KEEP_REGULAR_BUY' END,
              80,'FRESH',:minimum,:recommended,:reserved,:available,CAST(:request AS jsonb),CAST(:result AS jsonb),'SYSTEM')
            RETURNING "INV_DEC_ID"
            """).param("date",decision.decisionDate()).param("regime",decision.market().regime().name()).param("marketScore",decision.market().marketScore())
            .param("phase",decision.market().sentiment().phase().name()).param("riskScore",decision.market().sentiment().sentimentRiskScore())
            .param("minimum",decision.totalMinimumBuyAmount()).param("recommended",decision.totalRecommendedBuyAmount()).param("reserved",decision.newlyReservedCash())
            .param("available",decision.availableAdditionalBuyCash()).param("request",toJson(request)).param("result",toJson(decision)).query(Long.class).single();
        for(StockDecision stock:decision.stockDecisions()){
            jdbc.sql("""
              INSERT INTO "TB_STK_DEC"(
                "INV_DEC_ID","ACCT","STK_CD","STK_NM","ACT_SIG","FINAL_MULT","BASE_BUY_AMT","REG_BUY_AMT","SAVED_AMT","RSNS",
                "MAX_BUY_AMT","MAX_INC_MULT","ADJ_RSN","CASH_PLAN","ACCT_ID","STK_ID","HOLD_ID","BASE_DT","RISK_SCR","RISK_GRADE",
                "MKT_MULT","EXEC_YN","CONF_RT","DEC_RSN")
              SELECT :decisionId,:account,:code,:name,:action,:multiplier,:minimum,:recommended,:reserved,CAST(:reasons AS jsonb),
                :maximum,:maximumMultiplier,:adjustmentReason,:cashPlan,a."ACCT_ID",s."STK_ID",h."HOLD_ID",:date,
                d."RISK_SCR",d."RISK_GRADE",:multiplier,'N',d."CONF_RT",:adjustmentReason
              FROM "TB_STK" s JOIN "TB_ACCT" a ON a."ACCT_CD"=CASE WHEN s."MKT_CD"='US' THEN 'OVERSEAS_MAIN' ELSE 'DOMESTIC_MAIN' END
              LEFT JOIN "TB_HOLD" h ON h."ACCT_ID"=a."ACCT_ID" AND h."STK_ID"=s."STK_ID"
              JOIN "TB_INV_DEC" d ON d."INV_DEC_ID"=:decisionId WHERE s."STK_CD"=:code
              """).param("decisionId",id).param("date",decision.decisionDate()).param("account",stock.account()).param("code",stock.code()).param("name",stock.name())
              .param("action",stock.action().name()).param("multiplier",stock.multiplier()).param("minimum",stock.minimumBuyAmount()).param("maximum",stock.maximumBuyAmount())
              .param("maximumMultiplier",stock.maximumIncreaseMultiplier()).param("recommended",stock.recommendedBuyAmount()).param("reserved",stock.reservedCash())
              .param("adjustmentReason",stock.adjustmentReason()).param("cashPlan",stock.cashPlan()).param("reasons",toJson(stock.reasons())).update();
        }
    }
    private String toJson(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("의사결정 이력 JSON 변환에 실패했습니다.",e);}}
}
