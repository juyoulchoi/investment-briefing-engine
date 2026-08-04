package com.nanum.investment.marketdata;

import com.nanum.investment.calculation.*;
import com.nanum.investment.domain.DataStatus;
import org.slf4j.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.List;

@Service
public class MarketSentimentGenerationService {
 private static final Logger log=LoggerFactory.getLogger(MarketSentimentGenerationService.class);
 private static final BigDecimal FIFTY=new BigDecimal("50"),HUNDRED=new BigDecimal("100");
 private final JdbcClient jdbc;private final MarketSentimentCalculator calculator;
 public MarketSentimentGenerationService(JdbcClient jdbc,MarketSentimentCalculator calculator){this.jdbc=jdbc;this.calculator=calculator;}

 @Transactional public MarketSentimentGenerationResult generate(LocalDate baseDate){
  if(baseDate==null)throw new IllegalArgumentException("시장심리 기준일이 필요합니다.");
  List<SnapshotInput> inputs=inputs(baseDate);if(inputs.size()<2)throw new IllegalStateException("KR_MARKET·US_MARKET 스냅샷이 모두 필요합니다.");
  return new MarketSentimentGenerationResult(baseDate,inputs.stream().map(this::calculateAndSave).toList());
 }
 @Scheduled(cron="${market-sentiment.cron:0 45 7 * * MON-FRI}",zone="Asia/Seoul") public void scheduledGenerate(){try{generate(LocalDate.now(ZoneId.of("Asia/Seoul")));}catch(Exception e){log.error("시장심리 자동 생성 실패: {}",e.getMessage());}}

 private List<SnapshotInput> inputs(LocalDate day){return jdbc.sql("""
  SELECT s."BASE_DT",s."MKT_SNAP_CD",s."MKT_BREADTH_RT",s."LIQD_SCR",s."FRGN_NET_AMT",s."INST_NET_AMT",
   s."EXCH_CHG_RT",s."VOL_IDX_VAL",s."VOL_IDX_CHG_RT",s."MAIN_IDX_CHG_RT",s."DATA_STS",
   COALESCE((SELECT b."CHG_BP" FROM "TB_BOND_DAY" b WHERE b."BOND_CD"='DGS10' AND b."BASE_DT"<=s."BASE_DT" ORDER BY b."BASE_DT" DESC LIMIT 1),0)
  FROM "TB_MKT_SNAP" s WHERE s."BASE_DT"=:day AND s."MKT_SNAP_CD" IN ('KR_MARKET','US_MARKET')
   AND s."DATA_STS" IN ('FRESH','PARTIAL') ORDER BY s."MKT_SNAP_CD"
  """).param("day",day).query((rs,n)->new SnapshotInput(rs.getObject(1,LocalDate.class),rs.getString(2),rs.getBigDecimal(3),rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getBigDecimal(6),rs.getBigDecimal(7),rs.getBigDecimal(8),rs.getBigDecimal(9),rs.getBigDecimal(10),DataStatus.valueOf(rs.getString(11)),rs.getBigDecimal(12))).list();}

 private MarketSentimentGenerationResult.Sentiment calculateAndSave(SnapshotInput s){
  BigDecimal newsFear=negativePressure(s.indexChange(),new BigDecimal("5")),aiFatigue=FIFTY,earnings=FIFTY;
  BigDecimal liquidity=score(s.liquidity()==null?s.breadth():s.liquidity()),flow=flowScore(s.foreignNet(),s.institutionNet());
  BigDecimal exchange=positivePressure(s.exchangeChange(),new BigDecimal("3")),rate=positivePressure(s.rateChangeBp(),new BigDecimal("30")),volatility=volatilityPressure(s.volatility(),s.volatilityChange());
  BigDecimal confidence=s.status()==DataStatus.FRESH?new BigDecimal("85"):new BigDecimal("70");if(s.liquidity()==null)confidence=confidence.subtract(new BigDecimal("5"));if(s.foreignNet()==null&&s.institutionNet()==null)confidence=confidence.subtract(new BigDecimal("10"));
  var result=calculator.calculate(new MarketSentimentInput(newsFear,aiFatigue,earnings,liquidity,flow,exchange,rate,volatility,confidence));
  boolean structural=result.score().compareTo(new BigDecimal("20"))<0&&(volatility.compareTo(new BigDecimal("80"))>=0||s.indexChange()!=null&&s.indexChange().compareTo(new BigDecimal("-5"))<=0);
  String reason="확산도 "+one(liquidity)+", 수급 "+one(flow)+", 환율압력 "+one(exchange)+", 금리압력 "+one(rate)+", 변동성압력 "+one(volatility);
  Long id=jdbc.sql("""
   INSERT INTO "TB_MKT_SENT"("BASE_DT","MKT_SNAP_CD","NEWS_FEAR_SCR","AI_FATIGUE_SCR","EARN_CONF_SCR","LIQD_SCR","FLOW_SCR","EXCH_PRESS_SCR","RATE_PRESS_SCR","VOL_PRESS_SCR","SENT_SCR","SENT_PHASE","CONF_RT","STRUCT_DMG_YN","STRUCT_DMG_RT","KEY_RSN","RULE_VER_NO","DATA_STS")
   VALUES(:day,:code,:news,:fatigue,:earnings,:liquidity,:flow,:exchange,:rate,:volatility,:score,:phase,:confidence,:structural,:damage,:reason,1,:status)
   ON CONFLICT("BASE_DT","MKT_SNAP_CD") DO UPDATE SET "NEWS_FEAR_SCR"=EXCLUDED."NEWS_FEAR_SCR","AI_FATIGUE_SCR"=EXCLUDED."AI_FATIGUE_SCR","EARN_CONF_SCR"=EXCLUDED."EARN_CONF_SCR","LIQD_SCR"=EXCLUDED."LIQD_SCR","FLOW_SCR"=EXCLUDED."FLOW_SCR","EXCH_PRESS_SCR"=EXCLUDED."EXCH_PRESS_SCR","RATE_PRESS_SCR"=EXCLUDED."RATE_PRESS_SCR","VOL_PRESS_SCR"=EXCLUDED."VOL_PRESS_SCR","SENT_SCR"=EXCLUDED."SENT_SCR","SENT_PHASE"=EXCLUDED."SENT_PHASE","CONF_RT"=EXCLUDED."CONF_RT","STRUCT_DMG_YN"=EXCLUDED."STRUCT_DMG_YN","STRUCT_DMG_RT"=EXCLUDED."STRUCT_DMG_RT","KEY_RSN"=EXCLUDED."KEY_RSN","RULE_VER_NO"=EXCLUDED."RULE_VER_NO","DATA_STS"=EXCLUDED."DATA_STS","CALC_DTTM"=CURRENT_TIMESTAMP,"UPD_DTTM"=CURRENT_TIMESTAMP RETURNING "MKT_SENT_ID"
   """).param("day",s.date()).param("code",s.code()).param("news",newsFear).param("fatigue",aiFatigue).param("earnings",earnings).param("liquidity",liquidity).param("flow",flow).param("exchange",exchange).param("rate",rate).param("volatility",volatility).param("score",result.score()).param("phase",result.phase().name()).param("confidence",result.confidenceRate()).param("structural",structural?"Y":"N").param("damage",structural?new BigDecimal("80"):new BigDecimal("20")).param("reason",reason).param("status",s.status().name()).query(Long.class).single();
  return new MarketSentimentGenerationResult.Sentiment(id,s.code(),result.score(),result.phase(),result.confidenceRate(),structural,s.status(),reason);
 }
 private BigDecimal flowScore(BigDecimal foreignNet,BigDecimal institutionNet){if(foreignNet==null&&institutionNet==null)return FIFTY;return clamp(FIFTY.add(value(foreignNet).add(value(institutionNet)).divide(new BigDecimal("100000000000"),4,RoundingMode.HALF_UP)));}
 private BigDecimal volatilityPressure(BigDecimal level,BigDecimal change){if(level==null)return positivePressure(change,new BigDecimal("10"));return clamp(level.subtract(BigDecimal.TEN).multiply(new BigDecimal("3")).add(value(change).max(BigDecimal.ZERO)));}
 private BigDecimal negativePressure(BigDecimal change,BigDecimal scale){return clamp(value(change).negate().max(BigDecimal.ZERO).multiply(HUNDRED).divide(scale,4,RoundingMode.HALF_UP));}
 private BigDecimal positivePressure(BigDecimal change,BigDecimal scale){return clamp(value(change).max(BigDecimal.ZERO).multiply(HUNDRED).divide(scale,4,RoundingMode.HALF_UP));}
 private BigDecimal score(BigDecimal value){return value==null?FIFTY:clamp(value);}private BigDecimal value(BigDecimal value){return value==null?BigDecimal.ZERO:value;}private BigDecimal clamp(BigDecimal value){return value.max(BigDecimal.ZERO).min(HUNDRED).setScale(4,RoundingMode.HALF_UP);}private String one(BigDecimal value){return value.setScale(1,RoundingMode.HALF_UP).toPlainString();}
 private record SnapshotInput(LocalDate date,String code,BigDecimal breadth,BigDecimal liquidity,BigDecimal foreignNet,BigDecimal institutionNet,BigDecimal exchangeChange,BigDecimal volatility,BigDecimal volatilityChange,BigDecimal indexChange,DataStatus status,BigDecimal rateChangeBp){}
}
