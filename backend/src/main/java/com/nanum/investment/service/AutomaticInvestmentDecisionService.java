package com.nanum.investment.service;

import com.nanum.investment.api.InvestmentDecisionRequest;
import com.nanum.investment.domain.*;
import org.slf4j.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class AutomaticInvestmentDecisionService {
 private static final Logger log=LoggerFactory.getLogger(AutomaticInvestmentDecisionService.class);
 private final JdbcClient jdbc;private final PortfolioDecisionService decisions;private final DecisionHistoryService history;
 public AutomaticInvestmentDecisionService(JdbcClient jdbc,PortfolioDecisionService decisions,DecisionHistoryService history){this.jdbc=jdbc;this.decisions=decisions;this.history=history;}

 @Transactional public AutomaticInvestmentDecisionResult generate(LocalDate baseDate){
  if(baseDate==null)throw new IllegalArgumentException("투자판단 기준일이 필요합니다.");
  ensureSnapshots(baseDate);MarketSnapshot market=marketInput(baseDate);List<StockPosition> positions=positions(baseDate);
  if(positions.isEmpty())throw new IllegalStateException("활성 정기매수 종목이 없어 투자판단을 생성할 수 없습니다.");
  long reservedCash=reservedCash();InvestmentDecisionRequest request=new InvestmentDecisionRequest(baseDate,market,positions,reservedCash);
  PortfolioDecision decision=decisions.decide(request);history.save(request,decision);
  return new AutomaticInvestmentDecisionResult(baseDate,List.of("KR_MARKET","US_MARKET"),positions.size(),reservedCash,decision);
 }

 private void ensureSnapshots(LocalDate baseDate){
  List<String> codes=jdbc.sql("SELECT \"MKT_SNAP_CD\" FROM \"TB_MKT_SNAP\" WHERE \"BASE_DT\"=:day AND \"MKT_SNAP_CD\" IN ('KR_MARKET','US_MARKET') AND \"DATA_STS\" IN ('FRESH','PARTIAL')").param("day",baseDate).query(String.class).list();
  if(!codes.containsAll(List.of("KR_MARKET","US_MARKET")))throw new IllegalStateException("검증 완료된 KR_MARKET·US_MARKET 스냅샷이 모두 필요합니다.");
  Long sentiments=jdbc.sql("SELECT count(*) FROM \"TB_MKT_SENT\" WHERE \"BASE_DT\"=:day AND \"MKT_SNAP_CD\" IN ('KR_MARKET','US_MARKET','GLOBAL') AND \"DATA_STS\" IN ('FRESH','PARTIAL')").param("day",baseDate).query(Long.class).single();
  if(sentiments==0)throw new IllegalStateException("7단계 시장심리 결과가 없어 투자판단을 생성할 수 없습니다.");
 }

 private MarketSnapshot marketInput(LocalDate baseDate){
  return jdbc.sql("""
   WITH snapshots AS (SELECT s.*,d."DD_HIGH_RT" index_drawdown FROM "TB_MKT_SNAP" s
    LEFT JOIN LATERAL(SELECT x."DD_HIGH_RT" FROM "TB_IDX_DAY" x WHERE x."IDX_ID"=s."MAIN_IDX_ID" AND x."TRADE_DT"<=s."BASE_DT" ORDER BY x."TRADE_DT" DESC LIMIT 1)d ON true
    WHERE s."BASE_DT"=:day AND s."MKT_SNAP_CD" IN ('KR_MARKET','US_MARKET')),
   sentiment AS (SELECT avg(COALESCE("NEWS_FEAR_SCR",50)) news_fear,avg(COALESCE("AI_FATIGUE_SCR",50)) ai_fatigue,
    avg(COALESCE("EARN_CONF_SCR",50)) earnings,avg(COALESCE("LIQD_SCR",50)) liquidity
    FROM "TB_MKT_SENT" WHERE "BASE_DT"=:day AND "MKT_SNAP_CD" IN ('KR_MARKET','US_MARKET','GLOBAL')),
   bond AS (SELECT "CHG_BP" FROM "TB_BOND_DAY" WHERE "BOND_CD"='DGS10' AND "BASE_DT"<=:day ORDER BY "BASE_DT" DESC LIMIT 1)
   SELECT COALESCE(min(index_drawdown),0),COALESCE(max("VOL_IDX_VAL"),0),COALESCE(sum("FRGN_NET_AMT"),0),
    COALESCE(sum("INST_NET_AMT"),0),COALESCE(sum("INDV_NET_AMT"),0),COALESCE(sum("PGM_NET_AMT"),0),
    COALESCE(max("EXCH_CHG_RT"),0),COALESCE((SELECT "CHG_BP" FROM bond),0),COALESCE(avg("MKT_BREADTH_RT"),50),
    COALESCE((SELECT news_fear FROM sentiment),50),COALESCE((SELECT ai_fatigue FROM sentiment),50),
    COALESCE((SELECT earnings FROM sentiment),50),COALESCE((SELECT liquidity FROM sentiment),50)
   FROM snapshots
   """).param("day",baseDate).query((rs,n)->new MarketSnapshot(
    number(rs.getBigDecimal(1)),number(rs.getBigDecimal(2)),number(rs.getBigDecimal(3)),number(rs.getBigDecimal(4)),
    number(rs.getBigDecimal(5)),number(rs.getBigDecimal(6)),number(rs.getBigDecimal(7)),number(rs.getBigDecimal(8)),
    number(rs.getBigDecimal(9)),score(rs.getBigDecimal(10)),score(rs.getBigDecimal(11)),score(rs.getBigDecimal(12)),score(rs.getBigDecimal(13)),
    "한국·미국 시장 스냅샷과 확정 시장심리를 종합한 자동 투자판단")).single();
 }

 private List<StockPosition> positions(LocalDate baseDate){return jdbc.sql("""
  SELECT a."ACCT_TP",s."STK_CD",s."STK_NM",r."MIN_BUY_AMT",r."MIN_BUY_AMT"*3,3,
   COALESCE(h."PL_RT",0),COALESCE(p."DD_HIGH_RT",0),COALESCE(h."TGT_WGT",0),COALESCE(h."CUR_WGT",0),
   CASE WHEN s."FUND_DMG_YN"='Y' THEN 30 ELSE 70 END,50,CASE WHEN s."THEME_RISK_YN"='Y' THEN 80 ELSE 20 END,
   CASE WHEN s."REG_BUY_YN"='Y' AND r."BUY_STS"='ACTIVE' AND r."USER_PAUSE_YN"='N' THEN true ELSE false END
  FROM "TB_REG_BUY" r JOIN "TB_ACCT" a ON a."ACCT_ID"=r."ACCT_ID" JOIN "TB_STK" s ON s."STK_ID"=r."STK_ID"
  LEFT JOIN "TB_HOLD" h ON h."ACCT_ID"=a."ACCT_ID" AND h."STK_ID"=s."STK_ID" AND h."USE_YN"='Y' AND h."DEL_YN"='N'
  LEFT JOIN LATERAL(SELECT "DD_HIGH_RT" FROM "TB_PRC_DAY" d WHERE d."STK_ID"=s."STK_ID" AND d."TRADE_DT"<=:day ORDER BY d."TRADE_DT" DESC LIMIT 1)p ON true
  WHERE r."DEL_YN"='N' AND a."USE_YN"='Y' AND a."DEL_YN"='N' AND s."USE_YN"='Y' AND s."DEL_YN"='N'
  ORDER BY a."DISP_SEQ",s."STK_CD"
  """).param("day",baseDate).query((rs,n)->new StockPosition(rs.getString(1),rs.getString(2),rs.getString(3),amount(rs.getBigDecimal(4)),amount(rs.getBigDecimal(5)),number(rs.getBigDecimal(6)),number(rs.getBigDecimal(7)),number(rs.getBigDecimal(8)),weight(rs.getBigDecimal(9)),weight(rs.getBigDecimal(10)),number(rs.getBigDecimal(11)),number(rs.getBigDecimal(12)),number(rs.getBigDecimal(13)),rs.getBoolean(14))).list();}

 private long reservedCash(){BigDecimal value=jdbc.sql("SELECT COALESCE(sum(\"RSV_AMT\"),0) FROM \"TB_CASH_RSV\"").query(BigDecimal.class).single();return amount(value);}
 private int score(BigDecimal value){return (int)Math.max(0,Math.min(100,Math.round(number(value))));}
 private long amount(BigDecimal value){return value==null?0:value.setScale(0,java.math.RoundingMode.HALF_UP).longValue();}
 private double number(BigDecimal value){return value==null?0:value.doubleValue();}
 private double weight(BigDecimal value){double result=number(value);return result>1?result/100.0:result;}
}
