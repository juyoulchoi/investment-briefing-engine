package com.nanum.investment.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.response.BasicStockMetrics;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BasicStockMetricsCalculatorTest {
  private final BasicStockMetricsCalculator calculator = new BasicStockMetricsCalculator();

  @Test
  void calculatesExcelMetricsAsPercentages() {
    BasicStockMetrics result =
        calculator.calculate(
            new BigDecimal("10"),
            new BigDecimal("8000"),
            new BigDecimal("10000"),
            new BigDecimal("200000"),
            new BigDecimal("40"));

    assertThat(result.purchaseAmount()).isEqualByComparingTo("80000.00");
    assertThat(result.marketValue()).isEqualByComparingTo("100000.00");
    assertThat(result.profitAmount()).isEqualByComparingTo("20000.00");
    assertThat(result.stockReturnRate()).isEqualByComparingTo("25.0000");
    assertThat(result.currentWeight()).isEqualByComparingTo("50.0000");
    assertThat(result.weightDifference()).isEqualByComparingTo("10.0000");
  }

  @Test
  void returnsZeroRatesWhenDenominatorsAreZero() {
    BasicStockMetrics result =
        calculator.calculate(
            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO, null);

    assertThat(result.stockReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.currentWeight()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
