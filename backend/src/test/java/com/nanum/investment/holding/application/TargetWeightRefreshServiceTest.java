package com.nanum.investment.holding.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TargetWeightRefreshServiceTest {
  @Test
  void allocatesActiveHoldingByItsShareOfTheAccountScore() {
    BigDecimal target = TargetWeightRefreshService.target(8, 164, true, 6);

    assertThat(target).isEqualByComparingTo("0.048780");
  }

  @Test
  void assignsZeroToExcludedHolding() {
    BigDecimal target = TargetWeightRefreshService.target(8, 164, false, 6);

    assertThat(target).isEqualByComparingTo("0.000000");
  }

  @Test
  void leavesTargetUnsetWhenInvestmentGradeHasNoWeightScore() {
    assertThat(TargetWeightRefreshService.target(null, 164, true, 6)).isNull();
  }
}
