package com.nanum.investment.briefing.domain.calculator;

import com.nanum.investment.briefing.domain.FinalAction;
import com.nanum.investment.common.domain.RiskLevel;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.regularbuy.domain.RebuySignal;
import com.nanum.investment.regularbuy.domain.RegularBuySignal;
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
