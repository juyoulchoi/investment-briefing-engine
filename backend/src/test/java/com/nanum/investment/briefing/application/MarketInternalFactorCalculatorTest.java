package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MarketInternalFactorCalculatorTest {
  @Test
  void clampsDirectRatioToScoreRange() {
    assertThat(MarketInternalFactorCalculator.clamp(BigDecimal.valueOf(-2)))
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(MarketInternalFactorCalculator.clamp(BigDecimal.valueOf(57.25)))
        .isEqualByComparingTo("57.25");
    assertThat(MarketInternalFactorCalculator.clamp(BigDecimal.valueOf(102)))
        .isEqualByComparingTo("100");
  }
}
