package com.nanum.investment.service;

import com.nanum.investment.calculation.AdditionalBuyAllocator;
import com.nanum.investment.domain.*;
import com.nanum.investment.service.calculator.*;
import org.slf4j.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@Service
public class DailyBuyPlanService {
 private static final Logger log=LoggerFactory.getLogger(DailyBuyPlanService.class);
 private final JdbcClient jdbc;private final AdditionalBuyCalculator additional;private final RebuyCalculator rebuy;private final AdditionalBuyAllocator allocator;
 public DailyBuyPlanService(JdbcClient jdbc,AdditionalBuyCalculator additional,RebuyCalculator rebuy,AdditionalBuyAllocator allocator){this.jdbc=jdbc;this.additional=additional;this.rebuy=rebuy;this.allocator=allocator;}

 @Transactional public DailyBuyPlanResult calculateAndSave(LocalDate date){
  if(date==null)throw new IllegalArgumentException("계산 기준일이 필요합니다.");Decision decision=latestDecision(date);List<Candidate> candidates=candidates(decision.id(),date);if(candidates.isEmpty())throw new IllegalStateException("8단계 종목 투자판단 결과가 없습니다.");
  List<DailyBuyPlanResult.Plan> addPlans=new ArrayList<>(),rebuyPlans=new ArrayList<>();
  for(List<Candidate> accountCandidates:candidates.stream().collect(Collectors.groupingBy(Candidate::accountId,LinkedHashMap::new,Collectors.toList())).values()){
   BigDecimal budget=accountCandidates.getFirst().cash();List<AddEval> adds=accountCandidates.stream().map(c->additional(c,decision)).sorted(Comparator.comparing(AddEval::score).reversed()).toList();
   Map<Long,BigDecimal> allocated=allocator.allocate(budget,adds.stream().map(e->new AdditionalBuyAllocator.Candidate(e.candidate().stockDecisionId(),e.eligible(),e.score(),e.maximum(),e.candidate().weightGap())).toList()).stream().collect(Collectors.toMap(AdditionalBuyAllocator.Allocation::stockId,AdditionalBuyAllocator.Allocation::amount));
   int priority=1;BigDecimal used=BigDecimal.ZERO;Set<Long> selected=new HashSet<>();
   for(AddEval e:adds){BigDecimal amount=allocated.getOrDefault(e.candidate().stockDecisionId(),BigDecimal.ZERO);Integer rank=e.eligible()?priority++:null;if(amount.signum()>0)selected.add(e.candidate().stockDecisionId());used=used.add(amount);saveAdditional(date,e,rank,amount);addPlans.add(plan(e.candidate(),"ADDITIONAL_BUY",e.eligible(),rank,e.score(),amount,e.reason()));}
   BigDecimal remaining=budget.subtract(used).max(BigDecimal.ZERO);List<RebuyEval> rebuys=accountCandidates.stream().map(c->rebuy(c,decision,selected.contains(c.stockDecisionId()))).sorted(Comparator.comparing(RebuyEval::score).reversed()).toList();int rebuyPriority=1;
   for(RebuyEval e:rebuys){BigDecimal amount=rebuyAmount(e,remaining);remaining=remaining.subtract(amount);Integer rank=e.eligible()?rebuyPriority++:null;saveRebuy(date,e,rank,amount);rebuyPlans.add(plan(e.candidate(),dbSignal(e.signal()),e.eligible(),rank,e.score(),amount,e.reason()));}
  }
  BigDecimal addTotal=total(addPlans),rebuyTotal=total(rebuyPlans);jdbc.sql("UPDATE \"TB_INV_DEC\" SET \"ADD_BUY_TOT_AMT\"=:total WHERE \"INV_DEC_ID\"=:id").param("total",addTotal).param("id",decision.id()).update();
  return new DailyBuyPlanResult(date,decision.id(),addTotal,rebuyTotal,List.copyOf(addPlans),List.copyOf(rebuyPlans));
 }

 public List<Map<String,Object>> find(LocalDate date){return jdbc.sql("""
  SELECT 'ADDITIONAL_BUY' plan_type,a."ADD_BUY_ID" plan_id,a."BASE_DT" base_date,s."STK_CD" stock_code,s."STK_NM" stock_name,a."ELIG_YN" eligible_yn,a."PRIO_NO" priority,a."PRIO_SCR" score,a."RCMD_ADD_AMT" recommended_amount,a."ELIG_RSN" reason,a."EXEC_YN" executed_yn FROM "TB_ADD_BUY" a JOIN "TB_STK" s ON s."STK_ID"=a."STK_ID" WHERE a."BASE_DT"=:day
  UNION ALL SELECT 'REBUY',r."REBUY_ID",r."BASE_DT",s."STK_CD",s."STK_NM",r."ELIG_YN",r."PRIO_NO",r."REBUY_SCR",r."RCMD_REBUY_AMT",r."ELIG_RSN",r."EXEC_YN" FROM "TB_REBUY" r JOIN "TB_STK" s ON s."STK_ID"=r."STK_ID" WHERE r."BASE_DT"=:day ORDER BY plan_type,priority NULLS LAST,stock_code
  """).param("day",date).query().listOfRows();}

 private Decision latestDecision(LocalDate date){return jdbc.sql("SELECT \"INV_DEC_ID\",\"MKT_REGIME\",\"RISK_GRADE\" FROM \"TB_INV_DEC\" WHERE \"BASE_DT\"=:day AND \"LATEST_YN\"='Y' AND \"DATA_STS\" IN ('FRESH','PARTIAL') ORDER BY \"CALC_SEQ\" DESC LIMIT 1").param("day",date).query((rs,n)->new Decision(rs.getLong(1),MarketRegime.valueOf(rs.getString(2)),RiskGrade.valueOf(rs.getString(3)))).optional().orElseThrow(()->new IllegalStateException("8단계 최신 투자판단이 없습니다."));}
 private List<Candidate> candidates(Long id,LocalDate date){return jdbc.sql("""
  SELECT d."STK_DEC_ID",d."ACCT_ID",d."STK_ID",s."STK_CD",s."STK_NM",COALESCE(p."DD_HIGH_RT",0),CASE WHEN COALESCE(h."CUR_WGT",0)<COALESCE(h."TGT_WGT",0) THEN 'UNDER' ELSE 'NORMAL' END,
   COALESCE(r."MIN_BUY_AMT",0),COALESCE(r."MAX_BUY_AMT",r."MIN_BUY_AMT"*r."MAX_MULT",0),GREATEST((COALESCE(h."TGT_WGT",0)-COALESCE(h."CUR_WGT",0))/CASE WHEN COALESCE(h."TGT_WGT",0)>1 THEN 100 ELSE 1 END*COALESCE(t.total_asset,0),0),
   COALESCE(c."RSV_AMT",0),s."FUND_DMG_YN",s."ADD_BUY_YN",s."REBUY_YN",d."ACT_SIG",COALESCE(tr.latest>=tr.ma5,false),COALESCE(tr.latest>=tr.ma20,false)
  FROM "TB_STK_DEC" d JOIN "TB_STK" s ON s."STK_ID"=d."STK_ID" LEFT JOIN "TB_HOLD" h ON h."HOLD_ID"=d."HOLD_ID" LEFT JOIN "TB_REG_BUY" r ON r."ACCT_ID"=d."ACCT_ID" AND r."STK_ID"=d."STK_ID" AND r."USE_YN"='Y' AND r."DEL_YN"='N' LEFT JOIN "TB_CASH_RSV" c ON c."ACCT_ID"=d."ACCT_ID"
  LEFT JOIN LATERAL(SELECT sum(COALESCE(x."EVL_AMT",0))+max(a."CASH_AMT") total_asset FROM "TB_HOLD" x JOIN "TB_ACCT" a ON a."ACCT_ID"=x."ACCT_ID" WHERE x."ACCT_ID"=d."ACCT_ID" AND x."USE_YN"='Y' AND x."DEL_YN"='N')t ON true
  LEFT JOIN LATERAL(SELECT "DD_HIGH_RT" FROM "TB_PRC_DAY" x WHERE x."STK_ID"=d."STK_ID" AND x."TRADE_DT"<=:day ORDER BY x."TRADE_DT" DESC LIMIT 1)p ON true
  LEFT JOIN LATERAL(SELECT max("CLS_PRC") FILTER(WHERE rn=1) latest,avg("CLS_PRC") FILTER(WHERE rn<=5) ma5,avg("CLS_PRC") FILTER(WHERE rn<=20) ma20 FROM(SELECT "CLS_PRC",row_number() OVER(ORDER BY "TRADE_DT" DESC) rn FROM "TB_PRC_DAY" x WHERE x."STK_ID"=d."STK_ID" AND x."TRADE_DT"<=:day ORDER BY "TRADE_DT" DESC LIMIT 20)q)tr ON true
  WHERE d."INV_DEC_ID"=:id ORDER BY d."ACCT_ID",s."STK_CD"
  """).param("day",date).param("id",id).query((rs,n)->new Candidate(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getString(5),rs.getBigDecimal(6),WeightStatus.valueOf(rs.getString(7)),rs.getBigDecimal(8),rs.getBigDecimal(9),rs.getBigDecimal(10),rs.getBigDecimal(11),"Y".equals(rs.getString(12)),"Y".equals(rs.getString(13)),"Y".equals(rs.getString(14)),ActionSignal.valueOf(rs.getString(15)),rs.getBoolean(16),rs.getBoolean(17))).list();}

 private AddEval additional(Candidate c,Decision d){MarketPhase phase=phase(d);RiskLevel risk=risk(d);boolean dd=c.drawdown().compareTo(new BigDecimal("-10"))<=0,wgt=c.weight()==WeightStatus.UNDER,fund=!c.fundDamaged(),market=phase!=MarketPhase.NORMAL||c.drawdown().compareTo(new BigDecimal("-20"))<=0,cash=c.cash().signum()>0;BigDecimal maximum=additional.calculate(c.drawdown(),phase,c.weight(),risk,c.action()==ActionSignal.PAUSE,c.fundDamaged(),c.maximum(),c.cash());boolean eligible=c.addEligible()&&dd&&wgt&&fund&&market&&cash&&maximum.signum()>0;BigDecimal score=c.drawdown().abs().min(new BigDecimal("50")).add(wgt?new BigDecimal("25"):BigDecimal.ZERO).add(fund?new BigDecimal("15"):BigDecimal.ZERO).add(market?BigDecimal.TEN:BigDecimal.ZERO);return new AddEval(c,eligible,dd,wgt,fund,market,cash,score,maximum,eligible?"낙폭·비중·펀더멘털·시장·현금 조건 충족":"추가매수 필수조건 미충족");}
 private RebuyEval rebuy(Candidate c,Decision d,boolean addSelected){MarketPhase phase=phase(d);RiskLevel risk=risk(d);boolean fund=!c.fundDamaged(),market=phase==MarketPhase.NORMAL||phase==MarketPhase.CORRECTION,price=c.above5()||c.above20(),flow=market,wgt=c.weight()==WeightStatus.UNDER,cash=c.cash().signum()>0;RebuySignal signal=addSelected?RebuySignal.BLOCKED:rebuy.calculate(c.drawdown(),c.above5(),c.above20(),c.weight(),risk,phase,market,fund,c.rebuyEligible());boolean eligible=signal==RebuySignal.ALLOWED||signal==RebuySignal.PARTIAL;BigDecimal score=(c.above20()?new BigDecimal("35"):c.above5()?new BigDecimal("20"):BigDecimal.ZERO).add(market?new BigDecimal("20"):BigDecimal.ZERO).add(fund?new BigDecimal("20"):BigDecimal.ZERO).add(wgt?new BigDecimal("15"):BigDecimal.ZERO).add(cash?BigDecimal.TEN:BigDecimal.ZERO);return new RebuyEval(c,signal,eligible,fund,market,price,flow,wgt,cash,score,addSelected?"추가매수와 재매수 동시 생성 금지":"이동평균·시장·위험 해소 조건 평가");}

 private void saveAdditional(LocalDate date,AddEval e,Integer priority,BigDecimal amount){Candidate c=e.candidate();jdbc.sql("""
  INSERT INTO "TB_ADD_BUY"("STK_DEC_ID","ACCT_ID","STK_ID","BASE_DT","ELIG_YN","ELIG_RSN","DD_COND_YN","WGT_COND_YN","FUND_COND_YN","MKT_COND_YN","CASH_COND_YN","PRIO_NO","PRIO_SCR","MAX_ADD_AMT","WGT_GAP_AMT","CASH_AVAIL_AMT","RCMD_ADD_AMT","CASH_TP","EXEC_YN") VALUES(:decision,:account,:stock,:day,:eligible,:reason,:dd,:wgt,:fund,:market,:cash,:priority,:score,:maximum,:gap,:available,:amount,'RESERVE','N')
  ON CONFLICT("STK_DEC_ID") DO UPDATE SET "ELIG_YN"=EXCLUDED."ELIG_YN","ELIG_RSN"=EXCLUDED."ELIG_RSN","DD_COND_YN"=EXCLUDED."DD_COND_YN","WGT_COND_YN"=EXCLUDED."WGT_COND_YN","FUND_COND_YN"=EXCLUDED."FUND_COND_YN","MKT_COND_YN"=EXCLUDED."MKT_COND_YN","CASH_COND_YN"=EXCLUDED."CASH_COND_YN","PRIO_NO"=EXCLUDED."PRIO_NO","PRIO_SCR"=EXCLUDED."PRIO_SCR","MAX_ADD_AMT"=EXCLUDED."MAX_ADD_AMT","WGT_GAP_AMT"=EXCLUDED."WGT_GAP_AMT","CASH_AVAIL_AMT"=EXCLUDED."CASH_AVAIL_AMT","RCMD_ADD_AMT"=EXCLUDED."RCMD_ADD_AMT"
  """).param("decision",c.stockDecisionId()).param("account",c.accountId()).param("stock",c.stockId()).param("day",date).param("eligible",yn(e.eligible())).param("reason",e.reason()).param("dd",yn(e.dd())).param("wgt",yn(e.wgt())).param("fund",yn(e.fund())).param("market",yn(e.market())).param("cash",yn(e.cash())).param("priority",priority).param("score",e.score()).param("maximum",e.maximum()).param("gap",c.weightGap()).param("available",c.cash()).param("amount",amount).update();}
 private void saveRebuy(LocalDate date,RebuyEval e,Integer priority,BigDecimal amount){Candidate c=e.candidate();jdbc.sql("""
  INSERT INTO "TB_REBUY"("STK_DEC_ID","ACCT_ID","STK_ID","BASE_DT","ELIG_YN","FUND_OK_YN","MKT_OK_YN","PRICE_OK_YN","FLOW_OK_YN","WGT_OK_YN","CASH_OK_YN","REBUY_SCR","PRIO_NO","RCMD_REBUY_AMT","REBUY_SIG","ELIG_RSN","EXEC_YN") VALUES(:decision,:account,:stock,:day,:eligible,:fund,:market,:price,:flow,:wgt,:cash,:score,:priority,:amount,:signal,:reason,'N')
  ON CONFLICT("STK_DEC_ID") DO UPDATE SET "ELIG_YN"=EXCLUDED."ELIG_YN","FUND_OK_YN"=EXCLUDED."FUND_OK_YN","MKT_OK_YN"=EXCLUDED."MKT_OK_YN","PRICE_OK_YN"=EXCLUDED."PRICE_OK_YN","FLOW_OK_YN"=EXCLUDED."FLOW_OK_YN","WGT_OK_YN"=EXCLUDED."WGT_OK_YN","CASH_OK_YN"=EXCLUDED."CASH_OK_YN","REBUY_SCR"=EXCLUDED."REBUY_SCR","PRIO_NO"=EXCLUDED."PRIO_NO","RCMD_REBUY_AMT"=EXCLUDED."RCMD_REBUY_AMT","REBUY_SIG"=EXCLUDED."REBUY_SIG","ELIG_RSN"=EXCLUDED."ELIG_RSN"
  """).param("decision",c.stockDecisionId()).param("account",c.accountId()).param("stock",c.stockId()).param("day",date).param("eligible",yn(e.eligible())).param("fund",yn(e.fund())).param("market",yn(e.market())).param("price",yn(e.price())).param("flow",yn(e.flow())).param("wgt",yn(e.wgt())).param("cash",yn(e.cash())).param("score",e.score()).param("priority",priority).param("amount",amount).param("signal",dbSignal(e.signal())).param("reason",e.reason()).update();}

 private BigDecimal rebuyAmount(RebuyEval e,BigDecimal budget){if(!e.eligible()||budget.signum()<=0)return BigDecimal.ZERO;BigDecimal amount=e.signal()==RebuySignal.ALLOWED?e.candidate().minimum():e.candidate().minimum().multiply(new BigDecimal("0.5")).setScale(0,RoundingMode.DOWN);return amount.min(budget).min(e.candidate().weightGap().max(BigDecimal.ZERO));}
 private MarketPhase phase(Decision d){return switch(d.regime()){case NORMAL,OVERHEATED->MarketPhase.NORMAL;case MILD_CORRECTION->MarketPhase.CORRECTION;case STRONG_CORRECTION->MarketPhase.STRONG_CORRECTION;case CRASH_RISK->MarketPhase.CRASH;};}
 private RiskLevel risk(Decision d){return switch(d.risk()){case LOW->RiskLevel.LOW;case MEDIUM->RiskLevel.MEDIUM;case HIGH->RiskLevel.HIGH;case CRITICAL->RiskLevel.VERY_HIGH;};}
 private String dbSignal(RebuySignal signal){return switch(signal){case ALLOWED->"REBUY";case PARTIAL->"PARTIAL_REBUY";case BLOCKED->"BLOCKED";case WATCH->"WATCH";default->"WAIT";};}
 private DailyBuyPlanResult.Plan plan(Candidate c,String signal,boolean eligible,Integer priority,BigDecimal score,BigDecimal amount,String reason){return new DailyBuyPlanResult.Plan(c.stockDecisionId(),c.accountId(),c.stockId(),c.code(),signal,eligible,priority,score,amount,reason);}
 private BigDecimal total(List<DailyBuyPlanResult.Plan> plans){return plans.stream().map(DailyBuyPlanResult.Plan::recommendedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);}
 private String yn(boolean value){return value?"Y":"N";}
 private record Decision(Long id,MarketRegime regime,RiskGrade risk){}
 private record Candidate(Long stockDecisionId,Long accountId,Long stockId,String code,String name,BigDecimal drawdown,WeightStatus weight,BigDecimal minimum,BigDecimal maximum,BigDecimal weightGap,BigDecimal cash,boolean fundDamaged,boolean addEligible,boolean rebuyEligible,ActionSignal action,boolean above5,boolean above20){}
 private record AddEval(Candidate candidate,boolean eligible,boolean dd,boolean wgt,boolean fund,boolean market,boolean cash,BigDecimal score,BigDecimal maximum,String reason){}
 private record RebuyEval(Candidate candidate,RebuySignal signal,boolean eligible,boolean fund,boolean market,boolean price,boolean flow,boolean wgt,boolean cash,BigDecimal score,String reason){}
}
