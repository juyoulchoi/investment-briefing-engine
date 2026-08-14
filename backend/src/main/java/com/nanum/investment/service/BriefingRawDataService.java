package com.nanum.investment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.domain.DataStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class BriefingRawDataService {
 private final JdbcClient jdbc;private final ObjectMapper json;
 public BriefingRawDataService(JdbcClient jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}

 @Transactional public BriefingRawDataResult generate(LocalDate date){
  if(date==null)throw new IllegalArgumentException("브리핑 기준일이 필요합니다.");DecisionRef decision=decision(date);LinkedHashMap<String,Object> sections=sections(date,decision.id());DataStatus status=dataStatus(sections);int confidence=confidence(decision,sections);
  LinkedHashMap<String,Object> raw=new LinkedHashMap<>();raw.put("schemaVersion","1.0");raw.put("baseDate",date);raw.put("generatedAt",OffsetDateTime.now(ZoneId.of("Asia/Seoul")));raw.put("investmentDecisionId",decision.id());raw.put("dataStatus",status.name());raw.put("confidence",confidence);raw.put("sections",sections);
  String rawJson=toJson(raw),hash=sha256(rawJson);raw.put("sha256",hash);rawJson=toJson(raw);
  jdbc.sql("UPDATE \"TB_BRF\" SET \"LATEST_YN\"='N' WHERE \"BASE_DT\"=:day AND \"BRF_TP\"='DAILY' AND \"SCOPE_TP\"='GLOBAL' AND \"LATEST_YN\"='Y'").param("day",date).update();
  Integer sequence=jdbc.sql("SELECT COALESCE(max(\"CALC_SEQ\"),0)+1 FROM \"TB_BRF\" WHERE \"BASE_DT\"=:day AND \"BRF_TP\"='DAILY' AND \"SCOPE_TP\"='GLOBAL'").param("day",date).query(Integer.class).single();
  Long briefingId=jdbc.sql("""
   INSERT INTO "TB_BRF"("BASE_DT","CALC_SEQ","BRF_TP","SCOPE_TP","INV_DEC_ID","TITLE","BRF_STS","RAW_DATA_JSON","DATA_STS","CONF_RT","LATEST_YN","PUBL_YN","CRT_USR_ID","UPD_USR_ID")
   VALUES(:day,:sequence,'DAILY','GLOBAL',:decision,:title,'READY',CAST(:raw AS jsonb),:status,:confidence,'Y','N','SYSTEM','SYSTEM') RETURNING "BRF_ID"
   """).param("day",date).param("sequence",sequence).param("decision",decision.id()).param("title",date+" 일일 투자 브리핑").param("raw",rawJson).param("status",status.name()).param("confidence",confidence).query(Long.class).single();
  return new BriefingRawDataResult(briefingId,date,sequence,decision.id(),status,confidence,hash,List.copyOf(sections.keySet()));
 }

 public Map<String,Object> find(Long briefingId){List<Map<String,Object>> rows=jdbc.sql("SELECT \"BRF_ID\" briefing_id,\"BASE_DT\" base_date,\"CALC_SEQ\" calculation_sequence,\"BRF_STS\" briefing_status,\"DATA_STS\" data_status,\"CONF_RT\" confidence,\"RAW_DATA_JSON\" raw_data FROM \"TB_BRF\" WHERE \"BRF_ID\"=:id").param("id",briefingId).query().listOfRows();if(rows.isEmpty())throw new IllegalArgumentException("브리핑 원천데이터를 찾을 수 없습니다.");return rows.getFirst();}

 private LinkedHashMap<String,Object> sections(LocalDate date,Long decisionId){
  LinkedHashMap<String,Object> out=new LinkedHashMap<>();
  out.put("MARKET_SNAPSHOTS",rows("SELECT \"MKT_SNAP_CD\" snapshot_code,\"MKT_NM\" market_name,\"MAIN_IDX_VAL\" main_index_value,\"MAIN_IDX_CHG_RT\" main_index_change_rate,\"FRGN_NET_AMT\" foreign_net_amount,\"INST_NET_AMT\" institution_net_amount,\"INDV_NET_AMT\" individual_net_amount,\"PGM_NET_AMT\" program_net_amount,\"EXCH_RT\" exchange_rate,\"EXCH_CHG_RT\" exchange_change_rate,\"VOL_IDX_VAL\" volatility_index_value,\"ADV_STK_CNT\" advancing_count,\"DECL_STK_CNT\" declining_count,\"UNCH_STK_CNT\" unchanged_count,\"MKT_BREADTH_RT\" market_breadth_rate,\"TURNOVER_AMT\" turnover_amount,\"DATA_STS\" data_status FROM \"TB_MKT_SNAP\" WHERE \"BASE_DT\"=:day ORDER BY \"MKT_SNAP_CD\"",date,null));
  out.put("EXCHANGE_RATES",rows("SELECT \"BASE_CURR_CD\" base_currency,\"QUOTE_CURR_CD\" quote_currency,\"EXCH_RT\" exchange_rate,\"PREV_EXCH_RT\" previous_rate,\"CHG_AMT\" change_amount,\"CHG_RT\" change_rate,\"HIGH_52W_RT\" high_52week,\"LOW_52W_RT\" low_52week,\"DATA_STS\" data_status FROM \"TB_EXCH_DAY\" WHERE \"BASE_DT\"=(SELECT max(\"BASE_DT\") FROM \"TB_EXCH_DAY\" WHERE \"BASE_DT\"<=:day) ORDER BY \"BASE_CURR_CD\",\"QUOTE_CURR_CD\"",date,null));
  out.put("BOND_YIELDS",rows("SELECT \"BOND_CD\" bond_code,\"BOND_NM\" bond_name,\"YLD_RT\" yield_rate,\"PREV_YLD_RT\" previous_yield_rate,\"CHG_BP\" change_basis_points,\"DATA_STS\" data_status FROM \"TB_BOND_DAY\" WHERE \"BASE_DT\"=(SELECT max(\"BASE_DT\") FROM \"TB_BOND_DAY\" WHERE \"BASE_DT\"<=:day) ORDER BY \"BOND_CD\"",date,null));
  out.put("MARKET_SENTIMENT",rows("SELECT \"MKT_SNAP_CD\" snapshot_code,\"NEWS_FEAR_SCR\" news_fear_score,\"AI_FATIGUE_SCR\" ai_fatigue_score,\"EARN_CONF_SCR\" earnings_confidence_score,\"LIQD_SCR\" liquidity_score,\"FLOW_SCR\" flow_score,\"EXCH_PRESS_SCR\" exchange_pressure_score,\"RATE_PRESS_SCR\" rate_pressure_score,\"VOL_PRESS_SCR\" volatility_pressure_score,\"SENT_SCR\" sentiment_score,\"SENT_PHASE\" sentiment_phase,\"CONF_RT\" confidence,\"STRUCT_DMG_YN\" structural_damage_yn,\"REV_SIG\" reversal_signal,\"KEY_RSN\" key_reason,\"DATA_STS\" data_status FROM \"TB_MKT_SENT\" WHERE \"BASE_DT\"=:day ORDER BY \"MKT_SNAP_CD\"",date,null));
  out.put("INVESTMENT_DECISION",rows("SELECT \"INV_DEC_ID\" investment_decision_id,\"MKT_SCR\" market_score,\"MKT_REGIME\" market_regime,\"SENT_SCR\" sentiment_score,\"SENT_PHASE\" sentiment_phase,\"RISK_SCR\" risk_score,\"RISK_GRADE\" risk_grade,\"REG_BUY_TOT_AMT\" regular_buy_total,\"ADD_BUY_TOT_AMT\" additional_buy_total,\"RSV_ADD_AMT\" newly_reserved_amount,\"OVR_DEC_SIG\" overall_signal,\"CONF_RT\" confidence,\"DATA_STS\" data_status,\"KEY_RSN\" key_reason FROM \"TB_INV_DEC\" WHERE \"INV_DEC_ID\"=:id",date,decisionId));
  out.put("STOCK_DECISIONS",rows("SELECT s.\"STK_CD\" stock_code,s.\"STK_NM\" stock_name,a.\"ACCT_TP\" account_type,d.\"PL_RT\" profit_loss_rate,d.\"STK_DD_RT\" drawdown_rate,d.\"WGT_STS\" weight_status,d.\"RISK_GRADE\" risk_grade,d.\"FINAL_MULT\" final_multiplier,d.\"REG_BUY_AMT\" regular_buy_amount,d.\"SAVED_AMT\" saved_amount,d.\"ACT_SIG\" action_signal,d.\"CONF_RT\" confidence,d.\"DEC_RSN\" reason FROM \"TB_STK_DEC\" d JOIN \"TB_STK\" s ON s.\"STK_ID\"=d.\"STK_ID\" JOIN \"TB_ACCT\" a ON a.\"ACCT_ID\"=d.\"ACCT_ID\" WHERE d.\"INV_DEC_ID\"=:id ORDER BY a.\"DISP_SEQ\",s.\"STK_CD\"",date,decisionId));
  out.put("ADDITIONAL_BUYS",rows("SELECT s.\"STK_CD\" stock_code,a.\"ELIG_YN\" eligible_yn,a.\"ELIG_RSN\" reason,a.\"PRIO_SCR\" recommendation_score,a.\"MAX_ADD_AMT\" maximum_amount,a.\"WGT_GAP_AMT\" weight_gap_amount,a.\"CASH_AVAIL_AMT\" available_cash,a.\"RCMD_ADD_AMT\" recommended_amount FROM \"TB_ADD_BUY\" a JOIN \"TB_STK_DEC\" d ON d.\"STK_DEC_ID\"=a.\"STK_DEC_ID\" JOIN \"TB_STK\" s ON s.\"STK_ID\"=a.\"STK_ID\" LEFT JOIN \"TB_REG_BUY\" r ON r.\"ACCT_ID\"=a.\"ACCT_ID\" AND r.\"STK_ID\"=a.\"STK_ID\" AND r.\"DEL_YN\"='N' WHERE d.\"INV_DEC_ID\"=:id ORDER BY r.\"PRIORITY\" DESC NULLS LAST,a.\"PRIO_SCR\" DESC,s.\"STK_CD\"",date,decisionId));
  out.put("REBUYS",rows("SELECT s.\"STK_CD\" stock_code,r.\"ELIG_YN\" eligible_yn,r.\"REBUY_SIG\" rebuy_signal,r.\"ELIG_RSN\" reason,r.\"PRIO_NO\" priority,r.\"REBUY_SCR\" score,r.\"RCMD_REBUY_AMT\" recommended_amount FROM \"TB_REBUY\" r JOIN \"TB_STK_DEC\" d ON d.\"STK_DEC_ID\"=r.\"STK_DEC_ID\" JOIN \"TB_STK\" s ON s.\"STK_ID\"=r.\"STK_ID\" WHERE d.\"INV_DEC_ID\"=:id ORDER BY r.\"PRIO_NO\" NULLS LAST,s.\"STK_CD\"",date,decisionId));
  out.put("REBALANCING",rows("SELECT r.\"REBAL_TP\" rebalance_type,a.\"ACCT_TP\" account_type,s.\"STK_CD\" stock_code,s.\"STK_NM\" stock_name,i.\"CUR_AMT\" current_amount,i.\"TGT_AMT\" target_amount,i.\"CUR_WGT\" current_weight,i.\"TGT_WGT\" target_weight,i.\"CUR_REG_BUY_AMT\" current_regular_buy,i.\"NEW_REG_BUY_AMT\" new_regular_buy,i.\"REG_BUY_CHG_AMT\" change_amount,i.\"REBAL_ACT\" action,i.\"DEC_RSN\" reason FROM \"TB_REBAL\" r JOIN \"TB_REBAL_ITEM\" i ON i.\"REBAL_ID\"=r.\"REBAL_ID\" JOIN \"TB_ACCT\" a ON a.\"ACCT_ID\"=r.\"ACCT_ID\" JOIN \"TB_STK\" s ON s.\"STK_ID\"=i.\"STK_ID\" WHERE r.\"BASE_DT\"=:day AND r.\"LATEST_YN\"='Y' ORDER BY r.\"REBAL_TP\",a.\"DISP_SEQ\",i.\"PRIO_NO\"",date,null));
  out.put("ACCOUNTS",rows("SELECT a.\"ACCT_TP\" account_type,a.\"CASH_AMT\" cash_amount,a.\"RSV_CASH_AMT\" reserved_cash_amount,c.\"RSV_AMT\" additional_buy_cash,c.\"ACCUM_AMT\" accumulated_amount,c.\"USED_AMT\" used_amount FROM \"TB_ACCT\" a LEFT JOIN \"TB_CASH_RSV\" c ON c.\"ACCT_ID\"=a.\"ACCT_ID\" WHERE a.\"USE_YN\"='Y' AND a.\"DEL_YN\"='N' ORDER BY a.\"DISP_SEQ\"",date,null));
  out.put("DATA_QUALITY",quality(date,decisionId));validateRequired(out);return out;
 }

 private List<Map<String,Object>> rows(String sql,LocalDate date,Long id){var query=jdbc.sql(sql).param("day",date);if(id!=null)query=query.param("id",id);return query.query().listOfRows();}
 private Map<String,Object> quality(LocalDate date,Long id){Map<String,Object> result=new LinkedHashMap<>();result.put("baseDate",date);result.put("snapshotCount",count("TB_MKT_SNAP","BASE_DT",date));result.put("sentimentCount",count("TB_MKT_SENT","BASE_DT",date));result.put("stockDecisionCount",jdbc.sql("SELECT count(*) FROM \"TB_STK_DEC\" WHERE \"INV_DEC_ID\"=:id").param("id",id).query(Long.class).single());result.put("additionalBuyCount",countByDecision("TB_ADD_BUY",id));result.put("rebuyCount",countByDecision("TB_REBUY",id));return result;}
 private long count(String table,String column,LocalDate date){return jdbc.sql("SELECT count(*) FROM \""+table+"\" WHERE \""+column+"\"=:day").param("day",date).query(Long.class).single();}
 private long countByDecision(String table,Long id){return jdbc.sql("SELECT count(*) FROM \""+table+"\" x JOIN \"TB_STK_DEC\" d ON d.\"STK_DEC_ID\"=x.\"STK_DEC_ID\" WHERE d.\"INV_DEC_ID\"=:id").param("id",id).query(Long.class).single();}
 private void validateRequired(Map<String,Object> data){for(String key:List.of("MARKET_SNAPSHOTS","MARKET_SENTIMENT","INVESTMENT_DECISION","STOCK_DECISIONS","ADDITIONAL_BUYS","REBUYS")){if(data.get(key) instanceof Collection<?> values&&values.isEmpty())throw new IllegalStateException(key+" 원천데이터가 없습니다.");}}
 private DecisionRef decision(LocalDate date){return jdbc.sql("SELECT \"INV_DEC_ID\",\"CONF_RT\",\"DATA_STS\" FROM \"TB_INV_DEC\" WHERE \"BASE_DT\"=:day AND \"LATEST_YN\"='Y' ORDER BY \"CALC_SEQ\" DESC LIMIT 1").param("day",date).query((rs,n)->new DecisionRef(rs.getLong(1),rs.getInt(2),DataStatus.valueOf(rs.getString(3)))).optional().orElseThrow(()->new IllegalStateException("8단계 투자판단 결과가 없습니다."));}
 private DataStatus dataStatus(Map<String,Object> sections){for(Object section:sections.values())if(section instanceof List<?> rows)for(Object row:rows)if(row instanceof Map<?,?> map){Object status=map.get("data_status");if(status!=null&&!("FRESH".equals(status)||"PARTIAL".equals(status)))return DataStatus.ERROR;if("PARTIAL".equals(status))return DataStatus.PARTIAL;}return DataStatus.FRESH;}
 private int confidence(DecisionRef decision,Map<String,Object> sections){int value=decision.confidence();Object sentiment=sections.get("MARKET_SENTIMENT");if(sentiment instanceof List<?> rows)for(Object row:rows)if(row instanceof Map<?,?> map&&map.get("confidence") instanceof Number n)value=Math.min(value,n.intValue());return Math.max(0,Math.min(100,value));}
 private String toJson(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("브리핑 원천데이터 JSON 생성에 실패했습니다.",e);}}
 private String sha256(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(bytes);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private record DecisionRef(Long id,int confidence,DataStatus status){}
}
