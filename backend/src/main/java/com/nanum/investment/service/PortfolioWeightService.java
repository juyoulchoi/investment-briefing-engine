package com.nanum.investment.service;
import com.nanum.investment.domain.WeightStatus; import org.springframework.stereotype.Service; import java.math.*;
@Service public class PortfolioWeightService {
 public Result calculate(BigDecimal evaluationAmount,BigDecimal totalHoldings,BigDecimal cash,BigDecimal reserveCash,BigDecimal targetWeight){
  BigDecimal totalAsset=nvl(totalHoldings).add(nvl(cash)).add(nvl(reserveCash)); BigDecimal currentWeight=totalAsset.signum()==0?BigDecimal.ZERO:nvl(evaluationAmount).multiply(new BigDecimal("100")).divide(totalAsset,4,RoundingMode.HALF_UP);
  WeightStatus status=null; BigDecimal difference=null; if(targetWeight!=null&&targetWeight.signum()>0){difference=currentWeight.subtract(targetWeight); if(currentWeight.compareTo(targetWeight.multiply(new BigDecimal("0.8")))<0)status=WeightStatus.UNDERWEIGHT; else if(currentWeight.compareTo(targetWeight.multiply(new BigDecimal("1.2")))>0)status=WeightStatus.OVERWEIGHT; else status=WeightStatus.NORMAL;}
  return new Result(totalAsset,currentWeight,difference,status);
 } private BigDecimal nvl(BigDecimal v){return v==null?BigDecimal.ZERO:v;} public record Result(BigDecimal totalAsset,BigDecimal currentWeight,BigDecimal weightDifferenceRate,WeightStatus weightStatus){}
}
