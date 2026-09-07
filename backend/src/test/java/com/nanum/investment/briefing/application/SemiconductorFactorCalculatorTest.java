package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SemiconductorFactorCalculatorTest {
  @Test
  void calculatesReturnAndRelativeStrength() {
    double soxReturn =
        SemiconductorFactorCalculator.returnRate(
            BigDecimal.valueOf(110), BigDecimal.valueOf(100));
    double nasdaqReturn =
        SemiconductorFactorCalculator.returnRate(
            BigDecimal.valueOf(105), BigDecimal.valueOf(100));

    assertThat(soxReturn).isEqualTo(10);
    assertThat(nasdaqReturn).isEqualTo(5);
    assertThat(SemiconductorFactorCalculator.relativeStrength(soxReturn, nasdaqReturn))
        .isEqualTo(5);
  }
}
