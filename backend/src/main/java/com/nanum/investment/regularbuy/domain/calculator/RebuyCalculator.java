package com.nanum.investment.regularbuy.domain.calculator;

import com.nanum.investment.common.domain.RiskLevel;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.marketdata.domain.MarketPhase;
import com.nanum.investment.regularbuy.domain.RebuySignal;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RebuyCalculator {
  public RebuySignal calculate(
      BigDecimal drawdownRate,
      boolean aboveMa5,
      boolean aboveMa20,
      WeightStatus weightStatus,
      RiskLevel riskLevel,
      MarketPhase marketPhase,
      boolean benchmarkStable,
      boolean existingRiskResolved,
      boolean rebuyEligible) {
    if (!rebuyEligible
        || weightStatus != WeightStatus.UNDER
        || riskLevel == RiskLevel.VERY_HIGH
        || marketPhase == MarketPhase.STRONG_CORRECTION
        || marketPhase == MarketPhase.CRASH) return RebuySignal.WAIT;
    if (aboveMa20
        && benchmarkStable
        && existingRiskResolved
        && (marketPhase == MarketPhase.NORMAL || marketPhase == MarketPhase.CORRECTION)
        && riskLevel.ordinal() <= RiskLevel.MEDIUM.ordinal()) return RebuySignal.ALLOWED;
    if (drawdownRate != null
        && drawdownRate.compareTo(new BigDecimal("-15")) <= 0
        && aboveMa5
        && benchmarkStable
        && existingRiskResolved) return RebuySignal.PARTIAL;
    if (drawdownRate != null && drawdownRate.compareTo(new BigDecimal("-10")) <= 0)
      return RebuySignal.WATCH;
    return RebuySignal.WAIT;
  }
}
