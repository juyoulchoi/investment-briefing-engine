package com.nanum.investment.calculation;
import org.springframework.stereotype.Component; import java.math.*; import java.util.List;
@Component public class AccountAssetCalculator {
 public Result calculate(BigDecimal cash,BigDecimal reservedCash,List<Holding> holdings){
  nonNegative(cash);nonNegative(reservedCash); BigDecimal evaluation=BigDecimal.ZERO;
  for(Holding h:holdings==null?List.<Holding>of():holdings){nonNegative(h.quantity());nonNegative(h.currentPrice());if(h.exchangeRate()==null||h.exchangeRate().signum()<=0)throw new IllegalArgumentException("환율은 0보다 커야 합니다.");evaluation=evaluation.add(h.quantity().multiply(h.currentPrice()).multiply(h.exchangeRate()));}
  BigDecimal total=cash.add(reservedCash).add(evaluation),totalCash=cash.add(reservedCash);
  BigDecimal cashRate=total.signum()==0?BigDecimal.ZERO:totalCash.multiply(new BigDecimal("100")).divide(total,4,RoundingMode.HALF_UP);
  return new Result(total,evaluation,totalCash,cashRate);
 }
 private void nonNegative(BigDecimal v){if(v==null||v.signum()<0)throw new IllegalArgumentException("금액과 수량은 0 이상이어야 합니다.");}
 public record Holding(BigDecimal quantity,BigDecimal currentPrice,BigDecimal exchangeRate){}
 public record Result(BigDecimal totalAssetAmount,BigDecimal holdingEvaluationAmount,BigDecimal totalCashAmount,BigDecimal cashRate){}
}
