package com.nanum.investment.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.external.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class YahooIndexService {
 private final JdbcClient jdbc;
 private final RestClient client;
 private final ExternalApiRetryExecutor retry;
 public YahooIndexService(JdbcClient jdbc,@Value("${overseas.yahoo.base-url}") String baseUrl,ExternalRestClientFactory clients,ExternalApiRetryExecutor retry){
  this.jdbc=jdbc;this.retry=retry;this.client=clients.builder(baseUrl).defaultHeader("User-Agent","Mozilla/5.0 investment-briefing-engine/1.0").defaultHeader("Accept","application/json").build();
 }

 public CollectionResult refresh(String indexCode){return collect(indexCode,LocalDate.now().minusDays(10),LocalDate.now());}
 public CollectionResult collect(String requestedCode,LocalDate from,LocalDate to){
  if(from==null||to==null||from.isAfter(to))throw new IllegalArgumentException("유효하지 않은 조회 기간입니다.");
  IndexInfo index=findIndex(requestedCode);
  long period1=from.atStartOfDay(ZoneOffset.UTC).toEpochSecond(),period2=to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
  JsonNode result=fetch(index.sourceSymbol(),period1,period2);
  int saved=save(index,result,from,to);recalculate(index.id());
  return new CollectionResult(index.code(),index.sourceSymbol(),from,to,saved);
 }
 public List<CollectionResult> collectAll(LocalDate from,LocalDate to){return yahooIndices().stream().map(i->collect(i.code(),from,to)).toList();}
 public List<Map<String,Object>> indices(){return jdbc.sql("""
  SELECT "IDX_CD" index_code,"IDX_NM" index_name,"IDX_NM_EN" index_name_en,"SRC_SYMBOL" yahoo_symbol,
   "MKT_CD" market_code,"CNTRY_CD" country_code,"CURR_CD" currency_code,"DFLT_YN" default_yn
  FROM "TB_IDX" WHERE "DATA_SRC_CD"='YAHOO' AND "USE_YN"='Y' AND "DEL_YN"='N' ORDER BY "IDX_ID"
  """).query().listOfRows();}
 public List<Map<String,Object>> history(String requestedCode,LocalDate from,LocalDate to){
  IndexInfo index=findIndex(requestedCode);
  return jdbc.sql("""
   SELECT i."IDX_CD" index_code,i."IDX_NM" index_name,i."SRC_SYMBOL" yahoo_symbol,d."TRADE_DT" trade_date,
    d."OPEN_VAL" open_value,d."HIGH_VAL" high_value,d."LOW_VAL" low_value,d."CLS_VAL" close_value,
    d."PREV_CLS_VAL" previous_close,d."CHG_VAL" change_value,d."CHG_RT" change_rate,d."TRD_VOL" volume,
    d."HIGH_52W_VAL" high_52week,d."DD_52W_RT" drawdown_52week_rate,d."DATA_STS" data_status,d."COLLECT_DTTM" collected_at
   FROM "TB_IDX_DAY" d JOIN "TB_IDX" i ON i."IDX_ID"=d."IDX_ID"
   WHERE d."IDX_ID"=:id AND d."TRADE_DT" BETWEEN :from AND :to ORDER BY d."TRADE_DT"
   """).param("id",index.id()).param("from",from).param("to",to).query().listOfRows();
 }
 public Map<String,Object> latest(String requestedCode){
  IndexInfo index=findIndex(requestedCode);var rows=history(index.code(),LocalDate.now().minusYears(1),LocalDate.now());
  if(rows.isEmpty())throw new IllegalArgumentException("저장된 지수 시세가 없습니다.");return rows.getLast();
 }
 private List<IndexInfo> yahooIndices(){return jdbc.sql("""
  SELECT "IDX_ID","IDX_CD","IDX_NM","SRC_SYMBOL" FROM "TB_IDX"
  WHERE "DATA_SRC_CD"='YAHOO' AND "USE_YN"='Y' AND "DEL_YN"='N'
    AND "IDX_CD" IN ('SP500','NASDAQ_COMPOSITE','DOW_JONES','PHLX_SEMICONDUCTOR','VIX','NIKKEI225')
  ORDER BY "IDX_ID"
  """).query((rs,n)->new IndexInfo(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4))).list();}
 private IndexInfo findIndex(String requestedCode){
  String code=normalize(requestedCode);return jdbc.sql("""
   SELECT "IDX_ID","IDX_CD","IDX_NM","SRC_SYMBOL" FROM "TB_IDX"
   WHERE "IDX_CD"=:code AND "DATA_SRC_CD"='YAHOO' AND "USE_YN"='Y' AND "DEL_YN"='N'
   """).param("code",code).query((rs,n)->new IndexInfo(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4))).optional().orElseThrow(()->new IllegalArgumentException("등록된 Yahoo 지수가 아닙니다: "+code));
 }
 private JsonNode fetch(String symbol,long period1,long period2){
  JsonNode response=retry.execute(()->client.get().uri(uri->uri.pathSegment(symbol).queryParam("period1",period1).queryParam("period2",period2).queryParam("interval","1d").queryParam("events","div,splits").build()).retrieve().body(JsonNode.class));
  JsonNode chart=response==null?null:response.path("chart");
  if(chart==null||!chart.path("error").isNull()||chart.path("result").isEmpty())throw new IllegalStateException("Yahoo Finance 지수 시세를 받지 못했습니다: "+(chart==null?"빈 응답":chart.path("error")));
  return chart.path("result").get(0);
 }
 private int save(IndexInfo index,JsonNode result,LocalDate from,LocalDate to){
  JsonNode timestamps=result.path("timestamp"),quote=result.path("indicators").path("quote").get(0);ZoneId zone=ZoneId.of(result.path("meta").path("exchangeTimezoneName").asText("UTC"));int saved=0;BigDecimal previous=null;
  for(int i=0;i<timestamps.size();i++){
   JsonNode closeNode=value(quote.path("close"),i);if(closeNode==null||!closeNode.isNumber())continue;BigDecimal close=closeNode.decimalValue();LocalDate day=Instant.ofEpochSecond(timestamps.get(i).asLong()).atZone(zone).toLocalDate();if(day.isBefore(from)||day.isAfter(to)){previous=close;continue;}
   BigDecimal change=previous==null?null:close.subtract(previous),rate=previous==null||previous.signum()==0?null:change.multiply(new BigDecimal("100")).divide(previous,4,RoundingMode.HALF_UP);
   jdbc.sql("""
    INSERT INTO "TB_IDX_DAY"("TRADE_DT","IND_CD","IND_NM","CLS_VAL","CHG_VAL","CHG_RT","SRC_NM","OPEN_VAL","HIGH_VAL","LOW_VAL","TRD_VOL","IDX_ID","PREV_CLS_VAL","DATA_SRC_CD","DATA_STS")
    VALUES(:day,:code,:name,:close,:change,:rate,'YAHOO',:open,:high,:low,:volume,:id,:previous,'YAHOO','FRESH')
    ON CONFLICT("IDX_ID","TRADE_DT") DO UPDATE SET "IND_CD"=EXCLUDED."IND_CD","IND_NM"=EXCLUDED."IND_NM","CLS_VAL"=EXCLUDED."CLS_VAL","CHG_VAL"=EXCLUDED."CHG_VAL","CHG_RT"=EXCLUDED."CHG_RT","SRC_NM"='YAHOO',"OPEN_VAL"=EXCLUDED."OPEN_VAL","HIGH_VAL"=EXCLUDED."HIGH_VAL","LOW_VAL"=EXCLUDED."LOW_VAL","TRD_VOL"=EXCLUDED."TRD_VOL","PREV_CLS_VAL"=EXCLUDED."PREV_CLS_VAL","DATA_SRC_CD"='YAHOO',"DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP
    """).param("day",day).param("code",index.code()).param("name",index.name()).param("close",close).param("change",change).param("rate",rate).param("open",decimal(value(quote.path("open"),i))).param("high",decimal(value(quote.path("high"),i))).param("low",decimal(value(quote.path("low"),i))).param("volume",longValue(value(quote.path("volume"),i))).param("id",index.id()).param("previous",previous).update();previous=close;saved++;
  }return saved;
 }
 private void recalculate(long id){jdbc.sql("""
  WITH x AS (SELECT "IDX_DAY_ID",max("HIGH_VAL") OVER(PARTITION BY "IDX_ID" ORDER BY "TRADE_DT" ROWS BETWEEN 251 PRECEDING AND CURRENT ROW) h52,max("HIGH_VAL") OVER(PARTITION BY "IDX_ID") hall FROM "TB_IDX_DAY" WHERE "IDX_ID"=:id)
  UPDATE "TB_IDX_DAY" d SET "HIGH_52W_VAL"=x.h52,"DD_52W_RT"=CASE WHEN x.h52<>0 THEN (d."CLS_VAL"-x.h52)/x.h52*100 END,"ALL_HIGH_VAL"=x.hall,"DD_HIGH_RT"=CASE WHEN x.hall<>0 THEN (d."CLS_VAL"-x.hall)/x.hall*100 END FROM x WHERE d."IDX_DAY_ID"=x."IDX_DAY_ID"
  """).param("id",id).update();}
 private String normalize(String code){String value=code==null?"":code.trim().toUpperCase();if(!value.matches("[A-Z0-9_]{1,30}"))throw new IllegalArgumentException("유효하지 않은 지수 코드입니다.");return value;}
 private JsonNode value(JsonNode array,int i){return array==null||!array.isArray()||i>=array.size()?null:array.get(i);}
 private BigDecimal decimal(JsonNode node){return node==null||node.isNull()||!node.isNumber()?null:node.decimalValue();}
 private Long longValue(JsonNode node){return node==null||node.isNull()||!node.isNumber()?null:node.asLong();}
 private record IndexInfo(long id,String code,String name,String sourceSymbol){}
 public record CollectionResult(String indexCode,String yahooSymbol,LocalDate from,LocalDate to,int savedCount){}
}
