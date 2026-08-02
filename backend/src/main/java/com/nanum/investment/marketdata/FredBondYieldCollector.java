package com.nanum.investment.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.external.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component
public class FredBondYieldCollector implements BondYieldCollector {
 private static final Map<String,BondInfo> BONDS=Map.of(
  "DGS2",new BondInfo("미국 국채 2년",24),"DGS10",new BondInfo("미국 국채 10년",120),
  "DGS30",new BondInfo("미국 국채 30년",360),"DFII10",new BondInfo("미국 물가연동국채 실질금리 10년",120));
 private final RestClient client; private final String apiKey; private final ExternalApiRetryExecutor retry;
 public FredBondYieldCollector(@Value("${fred.base-url}") String baseUrl,@Value("${fred.api-key:}") String apiKey,ExternalRestClientFactory clients,ExternalApiRetryExecutor retry){this.client=clients.builder(baseUrl).defaultHeader("Accept","application/json").build();this.apiKey=apiKey;this.retry=retry;}
 @Override public Yield collect(String bondCode,LocalDate date){return collectRange(bondCode,date,date).stream().findFirst().orElseThrow(()->new IllegalStateException("해당 날짜의 FRED 채권금리가 없습니다."));}
 public List<Yield> collectRange(String requestedCode,LocalDate from,LocalDate to){
  if(apiKey==null||apiKey.isBlank())throw new IllegalStateException("FRED_API_KEY가 설정되지 않았습니다.");
  String code=requestedCode==null?"":requestedCode.trim().toUpperCase();BondInfo info=BONDS.get(code);if(info==null)throw new IllegalArgumentException("지원하지 않는 FRED 채권 코드입니다: "+code);
  JsonNode body=retry.execute(()->client.get().uri(u->u.path("/series/observations").queryParam("series_id",code).queryParam("api_key",apiKey).queryParam("file_type","json").queryParam("observation_start",from).queryParam("observation_end",to).queryParam("sort_order","asc").build()).retrieve().body(JsonNode.class));
  if(body==null||body.has("error_code"))throw new IllegalStateException("FRED 채권금리 응답을 처리하지 못했습니다.");
  List<Yield> result=new ArrayList<>();for(JsonNode item:body.path("observations")){String value=item.path("value").asText(".");if(".".equals(value))continue;result.add(new Yield(LocalDate.parse(item.path("date").asText()),code,info.name(),"US",info.months(),new BigDecimal(value),"FRED"));}return result;
 }
 public Set<String> supportedCodes(){return BONDS.keySet();}
 private record BondInfo(String name,Integer months){}
}
