package com.nanum.investment.regularbuy.application;

import java.math.*;
import org.springframework.stereotype.Service;

@Service
public class RegularBuyCalculationService {
  public Result calculate(
      BigDecimal minimum,
      BigDecimal maximum,
      BigDecimal market,
      BigDecimal stock,
      BigDecimal weight,
      BigDecimal risk,
      BigDecimal maximumMultiplier,
      BigDecimal availableCash,
      boolean paused) {
    if (paused) return new Result(BigDecimal.ZERO, BigDecimal.ZERO, nvl(minimum), "PAUSE");
    BigDecimal multiplier =
        nvl(market).multiply(nvlOne(stock)).multiply(nvlOne(weight)).multiply(nvlOne(risk));
    if (maximumMultiplier != null) multiplier = multiplier.min(maximumMultiplier);
    BigDecimal base = nvl(minimum);
    BigDecimal recommended = base.multiply(multiplier);
    if (maximum != null) recommended = recommended.min(maximum);
    if (availableCash != null) recommended = recommended.min(availableCash.max(BigDecimal.ZERO));
    return new Result(
        multiplier,
        recommended,
        base.subtract(recommended).max(BigDecimal.ZERO),
        multiplier.compareTo(BigDecimal.ONE) > 0
            ? "INCREASE"
            : multiplier.compareTo(BigDecimal.ONE) < 0 ? "REDUCE" : "KEEP_MINIMUM");
  }

  private BigDecimal nvl(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private BigDecimal nvlOne(BigDecimal v) {
    return v == null ? BigDecimal.ONE : v;
  }

  public record Result(
      BigDecimal finalMultiplier,
      BigDecimal recommendedBuyAmount,
      BigDecimal savedAmount,
      String actionSignal) {}
}
