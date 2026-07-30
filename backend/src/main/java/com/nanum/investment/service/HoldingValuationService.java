package com.nanum.investment.service;
import org.springframework.stereotype.Service; import java.math.*;
@Service public class HoldingValuationService {
 public Valuation calculate(BigDecimal quantity,BigDecimal averagePrice,BigDecimal currentPrice,BigDecimal exchangeRate){
  requireNonNegative(quantity,"보유수량"); requireNonNegative(averagePrice,"평단가"); requireNonNegative(currentPrice,"현재가"); if(exchangeRate==null||exchangeRate.signum()<=0) throw new IllegalArgumentException("환율은 0보다 커야 합니다.");
  BigDecimal originalEvaluation=quantity.multiply(currentPrice); BigDecimal evaluation=originalEvaluation.multiply(exchangeRate);
  BigDecimal originalProfitLoss=currentPrice.subtract(averagePrice).multiply(quantity); BigDecimal profitLoss=originalProfitLoss.multiply(exchangeRate);
  BigDecimal profitLossRate=averagePrice.signum()==0?null:currentPrice.subtract(averagePrice).multiply(new BigDecimal("100")).divide(averagePrice,4,RoundingMode.HALF_UP);
  return new Valuation(originalEvaluation,evaluation,originalProfitLoss,profitLoss,profitLossRate);
 }
 private void requireNonNegative(BigDecimal value,String name){if(value==null||value.signum()<0)throw new IllegalArgumentException(name+"은 0 이상이어야 합니다.");}
 public record Valuation(BigDecimal originalEvaluationAmount,BigDecimal evaluationAmount,BigDecimal originalProfitLossAmount,BigDecimal profitLossAmount,BigDecimal profitLossRate){}
}
