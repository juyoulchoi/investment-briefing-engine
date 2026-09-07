package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SectorPriceTrendFactorCalculatorTest {
  @Test
  void calculatesTwentyObservationReturnAsPercent() {
    assertThat(
            SectorPriceTrendFactorCalculator.returnRate(
                BigDecimal.valueOf(110), BigDecimal.valueOf(100)))
        .isEqualTo(10.0);
  }

  @Test
  void preservesNegativeReturnDirection() {
    assertThat(
            SectorPriceTrendFactorCalculator.returnRate(
                BigDecimal.valueOf(90), BigDecimal.valueOf(100)))
        .isEqualTo(-10.0);
  }
}
