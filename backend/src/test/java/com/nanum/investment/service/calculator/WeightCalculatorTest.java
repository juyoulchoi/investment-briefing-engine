package com.nanum.investment.service.calculator;

import com.nanum.investment.domain.WeightStatus;
import com.nanum.investment.response.WeightResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WeightCalculatorTest {
    private final WeightCalculator calculator = new WeightCalculator();

    @Test
    void calculatesMaximumWeightUsingTwentyPercentTolerance() {
        WeightResult result = calculator.calculate(
                new BigDecimal("84000"),
                new BigDecimal("1000000"),
                new BigDecimal("7"),
                new BigDecimal("0.20")
        );

        assertThat(result.currentWeight()).isEqualByComparingTo("8.4000");
        assertThat(result.targetWeight()).isEqualByComparingTo("7");
        assertThat(result.maximumWeight()).isEqualByComparingTo("8.4000");
        assertThat(result.status()).isEqualTo(WeightStatus.NORMAL);
    }

    @Test
    void marksWeightOverOnlyWhenItExceedsMaximumWeight() {
        WeightResult result = calculator.calculate(
                new BigDecimal("85000"),
                new BigDecimal("1000000"),
                new BigDecimal("7"),
                new BigDecimal("0.20")
        );

        assertThat(result.currentWeight()).isEqualByComparingTo("8.5000");
        assertThat(result.maximumWeight()).isEqualByComparingTo("8.4000");
        assertThat(result.status()).isEqualTo(WeightStatus.OVER);
    }
}
