package com.nanum.investment.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.domain.*;
import com.nanum.investment.request.RiskInput;
import com.nanum.investment.response.RiskResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RiskCalculatorTest {
  private final RiskCalculator calculator = new RiskCalculator();

  @Test
  void classifiesRiskLevelBoundaries() {
    assertThat(calculate(false, false, "0", WeightStatus.NORMAL, MarketPhase.NORMAL, false).level())
        .isEqualTo(RiskLevel.LOW);
    assertThat(calculate(true, false, "0", WeightStatus.NORMAL, MarketPhase.NORMAL, true).level())
        .isEqualTo(RiskLevel.MEDIUM);
    assertThat(calculate(true, true, "0", WeightStatus.NORMAL, MarketPhase.NORMAL, false).level())
        .isEqualTo(RiskLevel.HIGH);
    assertThat(calculate(true, true, "0", WeightStatus.OVER, MarketPhase.NORMAL, false).level())
        .isEqualTo(RiskLevel.VERY_HIGH);
  }

  @Test
  void accumulatesMinusThirtyPercentScores() {
    RiskResult result =
        calculate(false, false, "-30", WeightStatus.NORMAL, MarketPhase.NORMAL, false);

    assertThat(result.score()).isEqualTo(4);
    assertThat(result.level()).isEqualTo(RiskLevel.MEDIUM);
    assertThat(result.reasons()).contains("종목 수익률 -20% 이하 +2", "종목 수익률 -30% 이하 추가 +2");
  }

  private RiskResult calculate(
      boolean individualStock,
      boolean highRiskProduct,
      String stockReturnRate,
      WeightStatus weightStatus,
      MarketPhase marketPhase,
      boolean accumulationPaused) {
    return calculator.calculate(
        new RiskInput(
            individualStock,
            highRiskProduct,
            new BigDecimal(stockReturnRate),
            weightStatus,
            marketPhase,
            accumulationPaused));
  }
}
