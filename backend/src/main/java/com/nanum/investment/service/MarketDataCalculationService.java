package com.nanum.investment.service;

import com.nanum.investment.domain.DataStatus;
import java.math.*;
import java.time.*;
import org.springframework.stereotype.Service;

@Service
public class MarketDataCalculationService {
  public BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
    return previous == null || previous.signum() == 0
        ? null
        : current
            .subtract(previous)
            .multiply(new BigDecimal("100"))
            .divide(previous, 4, RoundingMode.HALF_UP);
  }

  public BigDecimal drawdownRate(BigDecimal current, BigDecimal high) {
    return high == null || high.signum() == 0
        ? null
        : current
            .subtract(high)
            .multiply(new BigDecimal("100"))
            .divide(high, 4, RoundingMode.HALF_UP);
  }

  public BigDecimal basisPointChange(BigDecimal currentYield, BigDecimal previousYield) {
    return previousYield == null
        ? null
        : currentYield.subtract(previousYield).multiply(new BigDecimal("100"));
  }

  public BigDecimal marketBreadth(Integer advancing, Integer declining) {
    if (advancing == null || declining == null || advancing + declining == 0) return null;
    return BigDecimal.valueOf(advancing)
        .multiply(new BigDecimal("100"))
        .divide(BigDecimal.valueOf(advancing + declining), 4, RoundingMode.HALF_UP);
  }

  public DataStatus freshness(
      OffsetDateTime collected,
      OffsetDateTime now,
      long freshMinutes,
      boolean partial,
      boolean error) {
    if (error) return DataStatus.ERROR;
    if (collected == null) return DataStatus.MISSING;
    if (partial) return DataStatus.PARTIAL;
    return Duration.between(collected, now).toMinutes() > freshMinutes
        ? DataStatus.STALE
        : DataStatus.FRESH;
  }
}
