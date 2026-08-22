package com.nanum.investment.briefing.domain.calculation;

import static org.assertj.core.api.Assertions.*;

import com.nanum.investment.marketdata.domain.MarketRegime;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvestmentCalculationEngineTest {
  private final MarketRuleSet rules = MarketRuleSet.defaultRules();
  private final MarketRegimeClassifier regimes = new MarketRegimeClassifier();

  @Test
  void marketRegimeBoundariesFollowPolicy() {
    assertThat(classify("85", false)).isEqualTo(MarketRegime.OVERHEATED);
    assertThat(classify("70", false)).isEqualTo(MarketRegime.NORMAL);
    assertThat(classify("50", false)).isEqualTo(MarketRegime.MILD_CORRECTION);
    assertThat(classify("30", false)).isEqualTo(MarketRegime.STRONG_CORRECTION);
    assertThat(classify("10", false)).isEqualTo(MarketRegime.CRASH_RISK);
  }

  @Test
  void emergencyOverridesHighScore() {
    assertThat(classify("95", true)).isEqualTo(MarketRegime.CRASH_RISK);
  }

  @Test
  void marketScoreAlwaysClampedForExtremeInputs() {
    MarketScoreCalculator calculator = new MarketScoreCalculator();
    for (String extreme : List.of("-100000", "0", "100000")) {
      BigDecimal x = bd(extreme);
      var result =
          calculator.calculate(
              new MarketScoreInput(x, x, x, x, x, x, x, x, x, x, x, x, x, x, bd("100")), rules);
      assertThat(result.totalScore()).isBetween(BigDecimal.ZERO, bd("100"));
    }
  }

  @Test
  void sentimentScoreAlwaysClamped() {
    MarketSentimentCalculator calculator = new MarketSentimentCalculator();
    var high =
        calculator.calculate(
            new MarketSentimentInput(
                bd("-1000"),
                bd("-1000"),
                bd("1000"),
                bd("1000"),
                bd("1000"),
                bd("-1000"),
                bd("-1000"),
                bd("-1000"),
                bd("100")));
    var low =
        calculator.calculate(
            new MarketSentimentInput(
                bd("1000"),
                bd("1000"),
                bd("-1000"),
                bd("-1000"),
                bd("-1000"),
                bd("1000"),
                bd("1000"),
                bd("1000"),
                bd("100")));
    assertThat(high.score()).isEqualByComparingTo("100");
    assertThat(low.score()).isZero();
  }

  @Test
  void additionalBuyAllocationNeverExceedsBudget() {
    var allocator = new AdditionalBuyAllocator();
    var allocations =
        allocator.allocate(
            bd("100"),
            List.of(
                new AdditionalBuyAllocator.Candidate(
                    1L, true, 20, bd("90"), "AAA", bd("80"), bd("70")),
                new AdditionalBuyAllocator.Candidate(
                    2L, true, 10, bd("80"), "BBB", bd("80"), bd("80"))));
    assertThat(
            allocations.stream()
                .map(AdditionalBuyAllocator.Allocation::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("100");
    assertThat(allocations.getFirst().stockId()).isEqualTo(1L);
  }

  @Test
  void accountAssetDoesNotCountReservedCashTwice() {
    var result =
        new AccountAssetCalculator()
            .calculate(
                bd("100"),
                bd("50"),
                List.of(new AccountAssetCalculator.Holding(bd("2"), bd("10"), bd("2"))));
    assertThat(result.totalAssetAmount()).isEqualByComparingTo("140");
    assertThat(result.holdingEvaluationAmount()).isEqualByComparingTo("40");
    assertThat(result.totalCashAmount()).isEqualByComparingTo("100");
  }

  private MarketRegime classify(String score, boolean emergency) {
    return regimes
        .classify(
            new MarketRegimeInput(
                bd(score),
                BigDecimal.ZERO,
                bd("50"),
                bd("20"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                emergency),
            rules)
        .value();
  }

  private BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
