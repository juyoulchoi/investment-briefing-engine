package com.nanum.investment.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.domain.MarketPhase;
import com.nanum.investment.response.MarketDrawdownResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MarketDrawdownCalculatorTest {
  private final MarketDrawdownCalculator calculator =
      new MarketDrawdownCalculator(new MarketPhaseCalculator());

  @Test
  void calculatesDrawdownFromRecentPeak() {
    MarketDrawdownResult result =
        calculator.calculate(new BigDecimal("800"), new BigDecimal("1000"));

    assertThat(result.drawdownRate()).isEqualByComparingTo("-20.0000");
    assertThat(result.marketPhase()).isEqualTo(MarketPhase.STRONG_CORRECTION);
  }

  @Test
  void classifiesEveryBoundary() {
    assertPhase("900", MarketPhase.CORRECTION);
    assertPhase("800", MarketPhase.STRONG_CORRECTION);
    assertPhase("700", MarketPhase.CRASH);
    assertPhase("901", MarketPhase.NORMAL);
  }

  private void assertPhase(String currentIndex, MarketPhase expected) {
    assertThat(
            calculator
                .calculate(new BigDecimal(currentIndex), new BigDecimal("1000"))
                .marketPhase())
        .isEqualTo(expected);
  }
}
