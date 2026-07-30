package com.nanum.investment.calculation;

import org.springframework.stereotype.Component;
import java.math.*; import java.util.*;

@Component
public class MarketScoreCalculator {
 private static final BigDecimal ZERO=BigDecimal.ZERO,HUNDRED=new BigDecimal("100");
 public MarketScoreResult calculate(MarketScoreInput i,MarketRuleSet r){
  require(i); BigDecimal trend=band(avg(i.mainIndexChangeRate(),i.subIndexChangeRate()),new String[]{"3","1","0","-1","-3"});
  BigDecimal flow=direction(weighted(i.foreignNetAmount(),i.institutionNetAmount()));
  BigDecimal futures=direction(weighted(i.foreignFuturesNetQuantity(),i.programNetAmount()));
  BigDecimal breadth=band(nvl(i.marketBreadthRate(),bd("50")),new String[]{"70","60","50","40","30"});
  BigDecimal volatility=clamp(bd("100").subtract(nvl(i.volatilityIndexValue(),bd("20")).multiply(bd("2.5")))
    .subtract(nvl(i.volatilityIndexChangeRate(),ZERO).max(ZERO).multiply(bd("2"))));
  BigDecimal exchange=clamp(bd("60").subtract(nvl(i.exchangeChangeRate(),ZERO).multiply(bd("15"))));
  BigDecimal rate=clamp(bd("100").subtract(nvl(i.usTwoYearYield(),ZERO).add(nvl(i.usTenYearYield(),ZERO)).multiply(bd("8"))));
  BigDecimal liquidity=clamp(nvl(i.liquidityScore(),bd("50")));
  BigDecimal total=weightedAverage(r,new BigDecimal[]{trend,flow,futures,breadth,volatility,exchange,rate,liquidity});
  List<CalculationReason> reasons=new ArrayList<>(); reasons.add(new CalculationReason("MARKET_SCORE","가중 시장점수 "+total));
  if(volatility.compareTo(bd("40"))<0)reasons.add(new CalculationReason("HIGH_VOLATILITY","변동성 급등 감점"));
  if(exchange.compareTo(bd("40"))<0)reasons.add(new CalculationReason("FX_PRESSURE","환율 상승 압력 감점"));
  return new MarketScoreResult(total,trend,flow,futures,breadth,volatility,exchange,rate,liquidity,clamp(nvl(i.dataConfidenceRate(),ZERO)),List.copyOf(reasons));
 }
 private BigDecimal weightedAverage(MarketRuleSet r,BigDecimal[] s){
  BigDecimal[] w={r.trendWeight(),r.flowWeight(),r.futuresProgramWeight(),r.breadthWeight(),r.volatilityWeight(),r.exchangeWeight(),r.rateWeight(),r.liquidityWeight()};
  BigDecimal sum=ZERO,weights=ZERO; for(int x=0;x<s.length;x++){sum=sum.add(s[x].multiply(w[x]));weights=weights.add(w[x]);}
  return clamp(sum.divide(weights,4,RoundingMode.HALF_UP));
 }
 private BigDecimal band(BigDecimal v,String[] t){if(v.compareTo(bd(t[0]))>=0)return HUNDRED;if(v.compareTo(bd(t[1]))>=0)return bd("80");if(v.compareTo(bd(t[2]))>=0)return bd("60");if(v.compareTo(bd(t[3]))>=0)return bd("40");if(v.compareTo(bd(t[4]))>=0)return bd("20");return ZERO;}
 private BigDecimal direction(BigDecimal v){if(v.signum()>0)return v.compareTo(bd("10000"))>=0?HUNDRED:bd("70");if(v.signum()<0)return v.compareTo(bd("-10000"))<=0?ZERO:bd("30");return bd("50");}
 private BigDecimal weighted(BigDecimal a,BigDecimal b){return nvl(a,ZERO).multiply(bd("0.65")).add(nvl(b,ZERO).multiply(bd("0.35")));}
 private BigDecimal avg(BigDecimal a,BigDecimal b){return nvl(a,ZERO).add(nvl(b,ZERO)).divide(bd("2"),4,RoundingMode.HALF_UP);}
 private BigDecimal clamp(BigDecimal v){return v.max(ZERO).min(HUNDRED).setScale(4,RoundingMode.HALF_UP);}
 private BigDecimal nvl(BigDecimal v,BigDecimal d){return v==null?d:v;} private BigDecimal bd(String v){return new BigDecimal(v);}
 private void require(MarketScoreInput i){if(i==null)throw new IllegalArgumentException("시장점수 입력은 필수입니다.");}
}
