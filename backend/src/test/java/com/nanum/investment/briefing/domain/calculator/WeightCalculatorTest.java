package com.nanum.investment.briefing.domain.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.briefing.api.response.WeightResult;
import com.nanum.investment.holding.domain.WeightStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WeightCalculatorTest {
  private final WeightCalculator calculator = new WeightCalculator();

  @Test
  void calculatesMaximumWeightUsingTwentyPercentTolerance() {
    WeightResult result =
        calculator.calculate(
            new BigDecimal("84000"),
            new BigDecimal("1000000"),
            new BigDecimal("7"),
            new BigDecimal("0.20"));

    assertThat(result.currentWeight()).isEqualByComparingTo("8.4000");
    assertThat(result.targetWeight()).isEqualByComparingTo("7");
    assertThat(result.maximumWeight()).isEqualByComparingTo("8.4000");
    assertThat(result.status()).isEqualTo(WeightStatus.NORMAL);
  }

  @Test
  void marksWeightOverOnlyWhenItExceedsMaximumWeight() {
    WeightResult result =
        calculator.calculate(
            new BigDecimal("85000"),
            new BigDecimal("1000000"),
            new BigDecimal("7"),
            new BigDecimal("0.20"));

    assertThat(result.currentWeight()).isEqualByComparingTo("8.5000");
    assertThat(result.maximumWeight()).isEqualByComparingTo("8.4000");
    assertThat(result.status()).isEqualTo(WeightStatus.OVER);
  }
}
