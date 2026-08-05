package com.nanum.investment.marketdata;

import com.nanum.investment.service.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class BriefingRefreshService {
 private final HoldingMarketDataRefreshService holdings;private final ExchangeRateService exchange;private final FredBondYieldService bonds;
 private final MarketDataConsistencyService validation;private final MarketSnapshotGenerationService snapshots;private final MarketSentimentGenerationService sentiments;
 private final AutomaticInvestmentDecisionService decisions;private final DailyBuyPlanService plans;private final AutomaticRebalanceService rebalancing;private final BriefingRawDataService rawData;
 public BriefingRefreshService(HoldingMarketDataRefreshService holdings,ExchangeRateService exchange,FredBondYieldService bonds,
  MarketDataConsistencyService validation,MarketSnapshotGenerationService snapshots,MarketSentimentGenerationService sentiments,
  AutomaticInvestmentDecisionService decisions,DailyBuyPlanService plans,AutomaticRebalanceService rebalancing,BriefingRawDataService rawData){this.holdings=holdings;this.exchange=exchange;this.bonds=bonds;this.validation=validation;this.snapshots=snapshots;this.sentiments=sentiments;this.decisions=decisions;this.plans=plans;this.rebalancing=rebalancing;this.rawData=rawData;}

 public BriefingRefreshResult refresh(){
  LocalDate day=LocalDate.now(ZoneId.of("Asia/Seoul"));List<String> completed=new ArrayList<>(),failures=new ArrayList<>();Map<String,Object> results=new LinkedHashMap<>();
  HoldingMarketDataRefreshResult market=holdings.refresh();results.put("MARKET_DATA",market);if(market.failures().isEmpty())completed.add("1-2 국내·해외·ETF 수집");else failures.addAll(market.failures());
  attempt("3 환율 수집",completed,failures,results,()->exchange.collect(day.minusDays(10),day));
  attempt("4 채권금리 수집",completed,failures,results,()->bonds.collect(day.minusDays(10),day));
  boolean quality=attempt("5 정합성 검증",completed,failures,results,()->validation.validateOrThrow(day));
  boolean snapshot=quality&&attempt("6 시장 스냅샷 생성",completed,failures,results,()->snapshots.generate(day));
  boolean sentiment=snapshot&&attempt("7 시장심리 계산",completed,failures,results,()->sentiments.generate(day));
  boolean decision=sentiment&&attempt("8 투자판단 생성",completed,failures,results,()->decisions.generate(day));
  boolean plan=decision&&attempt("9 추가매수·재매수 계산",completed,failures,results,()->plans.calculateAndSave(day));
  boolean rebalance=plan&&attempt("9 리밸런싱 계산",completed,failures,results,()->rebalancing.generate(day));
  if(rebalance)attempt("10 브리핑 원천데이터 생성",completed,failures,results,()->rawData.generate(day));
  return new BriefingRefreshResult(failures.isEmpty(),day,market.krxBaseDate(),market.krxReceivedCounts(),market.overseasRequestedCount(),market.overseasSuccessCount(),market.overseasSuccessSymbols(),List.copyOf(completed),Map.copyOf(results),List.copyOf(failures));
 }
 private boolean attempt(String name,List<String> completed,List<String> failures,Map<String,Object> results,Step step){try{results.put(name,step.run());completed.add(name);return true;}catch(Exception e){failures.add(name+": "+rootMessage(e));return false;}}
 private String rootMessage(Exception error){Throwable value=error;while(value.getCause()!=null)value=value.getCause();return value.getMessage()==null?value.getClass().getSimpleName():value.getMessage();}
 @FunctionalInterface private interface Step{Object run();}
}
