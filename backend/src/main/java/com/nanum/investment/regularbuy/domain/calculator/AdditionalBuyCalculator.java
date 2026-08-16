package com.nanum.investment.regularbuy.domain.calculator;

import com.nanum.investment.common.domain.RiskLevel;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.marketdata.domain.MarketPhase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class AdditionalBuyCalculator {
  public BigDecimal calculate(
      BigDecimal stockReturnRate,
      MarketPhase marketPhase,
      WeightStatus weightStatus,
      RiskLevel riskLevel,
      boolean paused,
      boolean fundamentalDamaged,
      BigDecimal baseAmount,
      BigDecimal availableCash) {
    if (paused
        || fundamentalDamaged
        || weightStatus != WeightStatus.UNDER
        || riskLevel == RiskLevel.VERY_HIGH
        || baseAmount == null
        || availableCash == null
        || availableCash.signum() <= 0) return BigDecimal.ZERO;
    BigDecimal recommended =
        baseAmount
            .multiply(stockDropAllocation(stockReturnRate).min(marketAllocation(marketPhase)))
            .setScale(0, RoundingMode.DOWN);
    return recommended.min(availableCash);
  }

  private BigDecimal stockDropAllocation(BigDecimal rate) {
    if (rate == null) return BigDecimal.ZERO;
    if (rate.compareTo(new BigDecimal("-30")) <= 0) return new BigDecimal("0.75");
    if (rate.compareTo(new BigDecimal("-20")) <= 0) return new BigDecimal("0.50");
    if (rate.compareTo(new BigDecimal("-10")) <= 0) return new BigDecimal("0.25");
    return BigDecimal.ZERO;
  }

  private BigDecimal marketAllocation(MarketPhase phase) {
    if (phase == null) return BigDecimal.ZERO;
    return switch (phase) {
      case NORMAL -> new BigDecimal("0.25");
      case CORRECTION -> new BigDecimal("0.50");
      case STRONG_CORRECTION -> new BigDecimal("0.75");
      case CRASH -> BigDecimal.ONE;
    };
  }
}
