package com.nanum.investment.service.calculator;

import com.nanum.investment.response.MarketDrawdownResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketDrawdownCalculator {
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private final MarketPhaseCalculator marketPhaseCalculator;

  public MarketDrawdownResult calculate(BigDecimal currentIndex, BigDecimal recentPeakIndex) {
    BigDecimal drawdownRate = BigDecimal.ZERO;
    if (currentIndex != null && recentPeakIndex != null && recentPeakIndex.signum() > 0) {
      drawdownRate =
          currentIndex
              .subtract(recentPeakIndex)
              .divide(recentPeakIndex, 8, RoundingMode.HALF_UP)
              .multiply(ONE_HUNDRED)
              .setScale(4, RoundingMode.HALF_UP);
    }
    return new MarketDrawdownResult(
        currentIndex == null ? BigDecimal.ZERO : currentIndex,
        recentPeakIndex == null ? BigDecimal.ZERO : recentPeakIndex,
        drawdownRate,
        marketPhaseCalculator.calculate(drawdownRate));
  }
}
