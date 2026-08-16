package com.nanum.investment.service.calculator;

import com.nanum.investment.domain.WeightStatus;
import com.nanum.investment.response.WeightResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class WeightCalculator {
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  public WeightResult calculate(
      BigDecimal marketValue,
      BigDecimal accountTotal,
      BigDecimal targetWeight,
      BigDecimal overToleranceRate) {
    if (accountTotal == null || accountTotal.signum() <= 0 || targetWeight == null) {
      return new WeightResult(
          BigDecimal.ZERO,
          targetWeight == null ? BigDecimal.ZERO : targetWeight,
          BigDecimal.ZERO,
          WeightStatus.NORMAL);
    }

    BigDecimal safeMarketValue = marketValue == null ? BigDecimal.ZERO : marketValue;
    BigDecimal tolerance = overToleranceRate == null ? BigDecimal.ZERO : overToleranceRate;
    BigDecimal currentWeight =
        safeMarketValue
            .divide(accountTotal, 8, RoundingMode.HALF_UP)
            .multiply(ONE_HUNDRED)
            .setScale(4, RoundingMode.HALF_UP);
    BigDecimal maximumWeight =
        targetWeight.multiply(BigDecimal.ONE.add(tolerance)).setScale(4, RoundingMode.HALF_UP);

    WeightStatus status;
    if (currentWeight.compareTo(maximumWeight) > 0) {
      status = WeightStatus.OVER;
    } else if (currentWeight.compareTo(targetWeight) < 0) {
      status = WeightStatus.UNDER;
    } else {
      status = WeightStatus.NORMAL;
    }
    return new WeightResult(currentWeight, targetWeight, maximumWeight, status);
  }
}
