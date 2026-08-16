package com.nanum.investment.briefing.domain.calculator;

import com.nanum.investment.briefing.api.response.BasicStockMetrics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class BasicStockMetricsCalculator {
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  public BasicStockMetrics calculate(
      BigDecimal quantity,
      BigDecimal averagePrice,
      BigDecimal currentPrice,
      BigDecimal accountTotalAmount,
      BigDecimal targetWeight) {
    BigDecimal safeQuantity = zeroIfNull(quantity);
    BigDecimal safeAveragePrice = zeroIfNull(averagePrice);
    BigDecimal safeCurrentPrice = zeroIfNull(currentPrice);
    BigDecimal safeTargetWeight = zeroIfNull(targetWeight);

    BigDecimal purchaseAmount =
        safeQuantity.multiply(safeAveragePrice).setScale(2, RoundingMode.HALF_UP);
    BigDecimal marketValue =
        safeQuantity.multiply(safeCurrentPrice).setScale(2, RoundingMode.HALF_UP);
    BigDecimal profitAmount =
        marketValue.subtract(purchaseAmount).setScale(2, RoundingMode.HALF_UP);

    BigDecimal stockReturnRate = BigDecimal.ZERO;
    if (safeAveragePrice.signum() > 0) {
      stockReturnRate =
          safeCurrentPrice
              .subtract(safeAveragePrice)
              .divide(safeAveragePrice, 8, RoundingMode.HALF_UP)
              .multiply(ONE_HUNDRED)
              .setScale(4, RoundingMode.HALF_UP);
    }

    BigDecimal currentWeight = BigDecimal.ZERO;
    if (accountTotalAmount != null && accountTotalAmount.signum() > 0) {
      currentWeight =
          marketValue
              .divide(accountTotalAmount, 8, RoundingMode.HALF_UP)
              .multiply(ONE_HUNDRED)
              .setScale(4, RoundingMode.HALF_UP);
    }

    BigDecimal weightDifference =
        currentWeight.subtract(safeTargetWeight).setScale(4, RoundingMode.HALF_UP);
    return new BasicStockMetrics(
        purchaseAmount,
        marketValue,
        profitAmount,
        stockReturnRate,
        currentWeight,
        weightDifference);
  }

  private BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
