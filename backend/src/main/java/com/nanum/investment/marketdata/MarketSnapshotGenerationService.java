package com.nanum.investment.marketdata;

import com.nanum.investment.domain.DataStatus;
import org.slf4j.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MarketSnapshotGenerationService {
 private static final Logger log=LoggerFactory.getLogger(MarketSnapshotGenerationService.class);
 private final MarketDataConsistencyService validation;private final JdbcClient jdbc;
 public MarketSnapshotGenerationService(MarketDataConsistencyService validation,JdbcClient jdbc){this.validation=validation;this.jdbc=jdbc;}

 @Transactional public MarketSnapshotGenerationResult generate(LocalDate baseDate){
  MarketDataValidationResult checked=validation.validateOrThrow(baseDate);
  Fx fx=latestFx(baseDate);SourceSnapshot kr=krSnapshot(baseDate,fx);SourceSnapshot us=usSnapshot(baseDate,fx);
  List<MarketSnapshotGenerationResult.Snapshot> saved=List.of(save(baseDate,kr,checked.dataStatus()),save(baseDate,us,checked.dataStatus()));
  return new MarketSnapshotGenerationResult(baseDate,checked.dataStatus(),checked.confidence(),saved,checked.warnings());
 }

 public List<Map<String,Object>> find(LocalDate baseDate){return jdbc.sql("""
  SELECT "MKT_SNAP_ID" snapshot_id,"BASE_DT" base_date,"MKT_SNAP_CD" snapshot_code,"MKT_NM" market_name,
   "MAIN_IDX_VAL" main_index_value,"MAIN_IDX_CHG_RT" main_index_change_rate,"EXCH_RT" exchange_rate,
   "EXCH_CHG_RT" exchange_change_rate,"VOL_IDX_VAL" volatility_index_value,"VOL_IDX_CHG_RT" volatility_index_change_rate,
   "ADV_STK_CNT" advancing_stock_count,"DECL_STK_CNT" declining_stock_count,"UNCH_STK_CNT" unchanged_stock_count,
   "MKT_BREADTH_RT" market_breadth_rate,"TURNOVER_AMT" turnover_amount,"DATA_SRC_CD" data_source_code,
   "DATA_STS" data_status,"DATA_AGE_MIN" data_age_minutes,"LAST_OK_DTTM" last_ok_at,"COLLECT_DTTM" collected_at,"RAW_REF" raw_reference
  FROM "TB_MKT_SNAP" WHERE "BASE_DT"=:day ORDER BY "MKT_SNAP_CD"
  """).param("day",baseDate).query().listOfRows();}

 @Scheduled(cron="${market-snapshot.cron:0 35 7 * * MON-FRI}",zone="Asia/Seoul") public void scheduledGenerate(){try{generate(LocalDate.now(ZoneId.of("Asia/Seoul")));}catch(Exception e){log.error("시장 스냅샷 자동 생성 실패: {}",e.getMessage());}}

 private SourceSnapshot krSnapshot(LocalDate baseDate,Fx fx){
  IndexPoint index=jdbc.sql("""
   SELECT i."IDX_ID",r.base_date,NULLIF(replace(r.payload->>'CLSPRC_IDX',',',''),'')::numeric,
    NULLIF(r.payload->>'FLUC_RT','')::numeric FROM tb_krx_data_row r JOIN "TB_IDX" i ON i."IDX_CD"='KOSPI'
   WHERE r.dataset_code='KOSPI_INDEX_DAILY' AND r.base_date<=:day AND r.payload->>'IDX_NM' IN ('코스피','KOSPI')
   ORDER BY r.base_date DESC LIMIT 1
   """).param("day",baseDate).query((rs,n)->new IndexPoint(rs.getLong(1),rs.getObject(2,LocalDate.class),rs.getBigDecimal(3),rs.getBigDecimal(4))).optional()
   .orElseGet(()->latestIndex("KOSPI",baseDate));
  Breadth breadth=jdbc.sql("""
   SELECT count(*) FILTER(WHERE NULLIF(payload->>'FLUC_RT','')::numeric>0),
    count(*) FILTER(WHERE NULLIF(payload->>'FLUC_RT','')::numeric<0),count(*) FILTER(WHERE NULLIF(payload->>'FLUC_RT','')::numeric=0),
    sum(NULLIF(replace(payload->>'ACC_TRDVAL',',',''),'')::numeric)
   FROM tb_krx_data_row WHERE dataset_code IN ('KOSPI_STOCK_DAILY','KOSDAQ_STOCK_DAILY','ETF_DAILY')
    AND base_date=(SELECT max(base_date) FROM tb_krx_data_row WHERE dataset_code='KOSPI_STOCK_DAILY' AND base_date<=:day)
   """).param("day",baseDate).query((rs,n)->new Breadth(rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getBigDecimal(4))).single();
  return new SourceSnapshot("KR_MARKET","한국시장",index,breadth,fx,null,"KRX,YAHOO");
 }

 private SourceSnapshot usSnapshot(LocalDate baseDate,Fx fx){
  IndexPoint index=latestIndex("SP500",baseDate);Volatility volatility=jdbc.sql("""
   SELECT d."TRADE_DT",d."CLS_VAL",d."CHG_RT" FROM "TB_IDX_DAY" d JOIN "TB_IDX" i ON i."IDX_ID"=d."IDX_ID"
   WHERE i."IDX_CD"='VIX' AND d."TRADE_DT"<=:day ORDER BY d."TRADE_DT" DESC LIMIT 1
   """).param("day",baseDate).query((rs,n)->new Volatility(rs.getObject(1,LocalDate.class),rs.getBigDecimal(2),rs.getBigDecimal(3))).optional().orElse(null);
  Breadth breadth=jdbc.sql("""
   WITH ranked AS (SELECT s."STK_ID",p."CLS_PRC",lag(p."CLS_PRC") OVER(PARTITION BY s."STK_ID" ORDER BY p."TRADE_DT") prev,
    row_number() OVER(PARTITION BY s."STK_ID" ORDER BY p."TRADE_DT" DESC) rn
    FROM "TB_STK" s JOIN "TB_PRC_DAY" p ON p."STK_ID"=s."STK_ID" WHERE s."LIST_SCOPE"='OVERSEAS' AND s."USE_YN"='Y' AND s."DEL_YN"='N' AND p."TRADE_DT"<=:day)
   SELECT count(*) FILTER(WHERE "CLS_PRC">prev),count(*) FILTER(WHERE "CLS_PRC"<prev),count(*) FILTER(WHERE "CLS_PRC"=prev),NULL::numeric FROM ranked WHERE rn=1
   """).param("day",baseDate).query((rs,n)->new Breadth(rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getBigDecimal(4))).single();
  return new SourceSnapshot("US_MARKET","미국시장",index,breadth,fx,volatility,"YAHOO");
 }

 private IndexPoint latestIndex(String code,LocalDate baseDate){return jdbc.sql("""
  SELECT i."IDX_ID",d."TRADE_DT",d."CLS_VAL",d."CHG_RT" FROM "TB_IDX_DAY" d JOIN "TB_IDX" i ON i."IDX_ID"=d."IDX_ID"
  WHERE i."IDX_CD"=:code AND d."TRADE_DT"<=:day ORDER BY d."TRADE_DT" DESC LIMIT 1
  """).param("code",code).param("day",baseDate).query((rs,n)->new IndexPoint(rs.getLong(1),rs.getObject(2,LocalDate.class),rs.getBigDecimal(3),rs.getBigDecimal(4))).optional().orElseThrow(()->new IllegalStateException(code+" 대표지수가 없습니다."));}
 private Fx latestFx(LocalDate baseDate){return jdbc.sql("SELECT \"BASE_DT\",\"EXCH_RT\",\"CHG_RT\" FROM \"TB_EXCH_DAY\" WHERE \"BASE_CURR_CD\"='USD' AND \"QUOTE_CURR_CD\"='KRW' AND \"BASE_DT\"<=:day ORDER BY \"BASE_DT\" DESC LIMIT 1").param("day",baseDate).query((rs,n)->new Fx(rs.getObject(1,LocalDate.class),rs.getBigDecimal(2),rs.getBigDecimal(3))).optional().orElseThrow(()->new IllegalStateException("USD/KRW 환율이 없습니다."));}

 private MarketSnapshotGenerationResult.Snapshot save(LocalDate baseDate,SourceSnapshot source,DataStatus status){
  BigDecimal breadthRate=breadthRate(source.breadth());int age=(int)Math.min(Integer.MAX_VALUE,ChronoUnit.MINUTES.between(source.index().date().atStartOfDay(),baseDate.plusDays(1).atStartOfDay()));
  Long id=jdbc.sql("""
   INSERT INTO "TB_MKT_SNAP"("BASE_DT","MKT_SNAP_CD","MKT_NM","MAIN_IDX_ID","MAIN_IDX_VAL","MAIN_IDX_CHG_RT",
    "EXCH_RT","EXCH_CHG_RT","VOL_IDX_VAL","VOL_IDX_CHG_RT","ADV_STK_CNT","DECL_STK_CNT","UNCH_STK_CNT","MKT_BREADTH_RT",
    "TURNOVER_AMT","DATA_SRC_CD","DATA_STS","DATA_AGE_MIN","LAST_OK_DTTM","RAW_REF")
   VALUES(:day,:code,:name,:indexId,:indexValue,:indexChange,:fx,:fxChange,:volatility,:volatilityChange,:advancing,:declining,:unchanged,:breadth,
    :turnover,:source,:status,:age,CURRENT_TIMESTAMP,:reference)
   ON CONFLICT("BASE_DT","MKT_SNAP_CD") DO UPDATE SET "MKT_NM"=EXCLUDED."MKT_NM","MAIN_IDX_ID"=EXCLUDED."MAIN_IDX_ID",
    "MAIN_IDX_VAL"=EXCLUDED."MAIN_IDX_VAL","MAIN_IDX_CHG_RT"=EXCLUDED."MAIN_IDX_CHG_RT","EXCH_RT"=EXCLUDED."EXCH_RT",
    "EXCH_CHG_RT"=EXCLUDED."EXCH_CHG_RT","VOL_IDX_VAL"=EXCLUDED."VOL_IDX_VAL","VOL_IDX_CHG_RT"=EXCLUDED."VOL_IDX_CHG_RT",
    "ADV_STK_CNT"=EXCLUDED."ADV_STK_CNT","DECL_STK_CNT"=EXCLUDED."DECL_STK_CNT","UNCH_STK_CNT"=EXCLUDED."UNCH_STK_CNT",
    "MKT_BREADTH_RT"=EXCLUDED."MKT_BREADTH_RT","TURNOVER_AMT"=EXCLUDED."TURNOVER_AMT","DATA_SRC_CD"=EXCLUDED."DATA_SRC_CD",
    "DATA_STS"=EXCLUDED."DATA_STS","DATA_AGE_MIN"=EXCLUDED."DATA_AGE_MIN","LAST_OK_DTTM"=CURRENT_TIMESTAMP,
    "COLLECT_DTTM"=CURRENT_TIMESTAMP,"RAW_REF"=EXCLUDED."RAW_REF","UPD_DTTM"=CURRENT_TIMESTAMP RETURNING "MKT_SNAP_ID"
   """).param("day",baseDate).param("code",source.code()).param("name",source.name()).param("indexId",source.index().id())
   .param("indexValue",source.index().value()).param("indexChange",source.index().change()).param("fx",source.fx().rate()).param("fxChange",source.fx().change())
   .param("volatility",source.volatility()==null?null:source.volatility().value()).param("volatilityChange",source.volatility()==null?null:source.volatility().change())
   .param("advancing",source.breadth().advancing()).param("declining",source.breadth().declining()).param("unchanged",source.breadth().unchanged())
   .param("breadth",breadthRate).param("turnover",source.breadth().turnover()).param("source",source.source()).param("status",status.name()).param("age",age)
   .param("reference","index="+source.index().date()+",fx="+source.fx().date()).query(Long.class).single();
  return new MarketSnapshotGenerationResult.Snapshot(id,source.code(),source.name(),source.index().date(),source.index().value(),source.index().change(),source.fx().rate(),source.volatility()==null?null:source.volatility().value(),source.breadth().advancing(),source.breadth().declining(),source.breadth().unchanged(),breadthRate);
 }
 private BigDecimal breadthRate(Breadth value){int directional=value.advancing()+value.declining();return directional==0?null:BigDecimal.valueOf(value.advancing()).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(directional),4,RoundingMode.HALF_UP);}
 private record IndexPoint(Long id,LocalDate date,BigDecimal value,BigDecimal change){}
 private record Fx(LocalDate date,BigDecimal rate,BigDecimal change){}
 private record Volatility(LocalDate date,BigDecimal value,BigDecimal change){}
 private record Breadth(int advancing,int declining,int unchanged,BigDecimal turnover){}
 private record SourceSnapshot(String code,String name,IndexPoint index,Breadth breadth,Fx fx,Volatility volatility,String source){}
}
