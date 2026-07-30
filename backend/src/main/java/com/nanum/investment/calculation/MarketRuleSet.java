package com.nanum.investment.calculation;
import java.math.BigDecimal;
public record MarketRuleSet(BigDecimal trendWeight,BigDecimal flowWeight,BigDecimal futuresProgramWeight,
 BigDecimal breadthWeight,BigDecimal volatilityWeight,BigDecimal exchangeWeight,BigDecimal rateWeight,
 BigDecimal liquidityWeight,BigDecimal structuralDamageCrashThreshold){
 public static MarketRuleSet defaultRules(){return new MarketRuleSet(bd("15"),bd("20"),bd("15"),bd("15"),bd("15"),bd("10"),bd("5"),bd("5"),bd("30"));}
 private static BigDecimal bd(String v){return new BigDecimal(v);}
}
