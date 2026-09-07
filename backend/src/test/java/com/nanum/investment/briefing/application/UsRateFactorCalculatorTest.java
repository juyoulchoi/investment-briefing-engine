package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsRateFactorCalculatorTest {
  @Test
  void invertsRatePressureScore() {
    assertThat(UsRateFactorCalculator.inverseScore(80)).isEqualTo(20);
    assertThat(UsRateFactorCalculator.inverseScore(50)).isEqualTo(50);
    assertThat(UsRateFactorCalculator.inverseScore(15)).isEqualTo(85);
  }
}
