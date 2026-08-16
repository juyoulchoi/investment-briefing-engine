package com.nanum.investment.briefing.domain.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.briefing.domain.FinalAction;
import com.nanum.investment.common.domain.RiskLevel;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.marketdata.domain.MarketPhase;
import com.nanum.investment.regularbuy.domain.RebuySignal;
import com.nanum.investment.regularbuy.domain.RegularBuySignal;
import com.nanum.investment.regularbuy.domain.calculator.AdditionalBuyCalculator;
import com.nanum.investment.regularbuy.domain.calculator.RebuyCalculator;
import com.nanum.investment.regularbuy.domain.calculator.RegularBuyCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvestmentSignalCalculatorTest {
  private final RegularBuyCalculator regular = new RegularBuyCalculator();
  private final AdditionalBuyCalculator additional = new AdditionalBuyCalculator();
  private final RebuyCalculator rebuy = new RebuyCalculator();
  private final FinalActionCalculator action = new FinalActionCalculator();

  @Test
  void regularBuyChecksMarketCalendar() {
    assertThat(
            regular.calculate(
                LocalDate.of(2026, 7, 27),
                false,
                "DAILY",
                null,
                null,
                false,
                WeightStatus.UNDER,
                RiskLevel.LOW,
                BigDecimal.TEN,
                BigDecimal.TEN))
        .isEqualTo(RegularBuySignal.NOT_SCHEDULED);
    assertThat(
            regular.calculate(
                LocalDate.of(2026, 7, 27),
                true,
                "DAILY",
                null,
                null,
                false,
                WeightStatus.UNDER,
                RiskLevel.LOW,
                BigDecimal.TEN,
                BigDecimal.TEN))
        .isEqualTo(RegularBuySignal.EXECUTE);
  }

  @Test
  void regularBuyUsesStandardScheduleCodes() {
    assertThat(
            regular.calculate(
                LocalDate.of(2026, 7, 27),
                true,
                "WEEKLY",
                "MON,WED,FRI",
                null,
                false,
                WeightStatus.UNDER,
                RiskLevel.LOW,
                BigDecimal.TEN,
                BigDecimal.TEN))
        .isEqualTo(RegularBuySignal.EXECUTE);
    assertThat(
            regular.calculate(
                LocalDate.of(2026, 7, 28),
                true,
                "WEEKLY",
                "MON,WED,FRI",
                null,
                false,
                WeightStatus.UNDER,
                RiskLevel.LOW,
                BigDecimal.TEN,
                BigDecimal.TEN))
        .isEqualTo(RegularBuySignal.NOT_SCHEDULED);
    assertThat(
            regular.calculate(
                LocalDate.of(2026, 7, 15),
                true,
                "MONTHLY",
                null,
                15,
                false,
                WeightStatus.UNDER,
                RiskLevel.LOW,
                BigDecimal.TEN,
                BigDecimal.TEN))
        .isEqualTo(RegularBuySignal.EXECUTE);
  }

  @Test
  void additionalBuyUsesMinimumAllocationAndRequiresUnderWeight() {
    assertThat(
            additional.calculate(
                new BigDecimal("-25"),
                MarketPhase.CORRECTION,
                WeightStatus.UNDER,
                RiskLevel.MEDIUM,
                false,
                false,
                new BigDecimal("100000"),
                new BigDecimal("100000")))
        .isEqualByComparingTo("50000");
    assertThat(
            additional.calculate(
                new BigDecimal("-25"),
                MarketPhase.CRASH,
                WeightStatus.NORMAL,
                RiskLevel.MEDIUM,
                false,
                false,
                new BigDecimal("100000"),
                new BigDecimal("100000")))
        .isZero();
  }

  @Test
  void rebuyRequiresRecoveryAndStableBenchmark() {
    assertThat(
            rebuy.calculate(
                new BigDecimal("-16"),
                true,
                false,
                WeightStatus.UNDER,
                RiskLevel.MEDIUM,
                MarketPhase.CORRECTION,
                true,
                true,
                true))
        .isEqualTo(RebuySignal.PARTIAL);
    assertThat(
            rebuy.calculate(
                new BigDecimal("-16"),
                true,
                true,
                WeightStatus.UNDER,
                RiskLevel.MEDIUM,
                MarketPhase.NORMAL,
                true,
                true,
                true))
        .isEqualTo(RebuySignal.ALLOWED);
    assertThat(
            rebuy.calculate(
                new BigDecimal("-16"),
                true,
                true,
                WeightStatus.UNDER,
                RiskLevel.LOW,
                MarketPhase.STRONG_CORRECTION,
                true,
                true,
                true))
        .isEqualTo(RebuySignal.WAIT);
  }

  @Test
  void finalActionUsesRequiredPriority() {
    assertThat(
            action.calculate(
                RiskLevel.VERY_HIGH,
                WeightStatus.OVER,
                BigDecimal.TEN,
                RegularBuySignal.EXECUTE,
                RebuySignal.ALLOWED))
        .isEqualTo(FinalAction.STOP_BUY);
    assertThat(
            action.calculate(
                RiskLevel.LOW,
                WeightStatus.OVER,
                BigDecimal.TEN,
                RegularBuySignal.EXECUTE,
                RebuySignal.ALLOWED))
        .isEqualTo(FinalAction.REDUCE_WEIGHT);
    assertThat(
            action.calculate(
                RiskLevel.LOW,
                WeightStatus.UNDER,
                BigDecimal.TEN,
                RegularBuySignal.EXECUTE,
                RebuySignal.ALLOWED))
        .isEqualTo(FinalAction.ADDITIONAL_BUY);
  }
}
