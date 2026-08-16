package com.nanum.investment.service.calculator;

import com.nanum.investment.domain.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FinalActionCalculator {
  public FinalAction calculate(
      RiskLevel riskLevel,
      WeightStatus weightStatus,
      BigDecimal additionalBuyAmount,
      RegularBuySignal regularBuySignal,
      RebuySignal rebuySignal) {
    if (riskLevel == RiskLevel.VERY_HIGH) return FinalAction.STOP_BUY;
    if (weightStatus == WeightStatus.OVER) return FinalAction.REDUCE_WEIGHT;
    if (additionalBuyAmount != null && additionalBuyAmount.signum() > 0) {
      return FinalAction.ADDITIONAL_BUY;
    }
    if (regularBuySignal == RegularBuySignal.EXECUTE) return FinalAction.REGULAR_BUY;
    if (rebuySignal == RebuySignal.PARTIAL || rebuySignal == RebuySignal.ALLOWED) {
      return FinalAction.REBUY_PARTIAL;
    }
    return FinalAction.HOLD;
  }
}
