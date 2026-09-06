package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RobustZScoreCalculatorTest {

  @Test
  void calculatesMedianMadAndNormalizedScoreWithoutBeingDistortedByOutlier() {
    List<Double> history = new ArrayList<>();
    for (int value = 1; value <= 20; value++) {
      history.add((double) value);
    }
    history.set(19, 1_000_000d);

    RobustZScoreCalculator.Result result = RobustZScoreCalculator.calculate(12, history);

    assertThat(result.status()).isEqualTo(RobustZScoreCalculator.Status.AVAILABLE);
    assertThat(result.sampleCount()).isEqualTo(20);
    assertThat(result.median()).isEqualTo(10.5);
    assertThat(result.medianAbsoluteDeviation()).isEqualTo(5.0);
    assertThat(result.rawZScore()).isCloseTo(0.202346925, within(0.000000001));
    assertThat(result.normalizedScore()).isCloseTo(52.02346925, within(0.00000001));
  }

  @Test
  void capsExtremeZScoreBeforeConvertingToZeroToOneHundredScore() {
    List<Double> history = new ArrayList<>();
    for (int value = 1; value <= 20; value++) {
      history.add((double) value);
    }

    RobustZScoreCalculator.Result result = RobustZScoreCalculator.calculate(1_000_000, history);

    assertThat(result.cappedZScore()).isEqualTo(3.5);
    assertThat(result.normalizedScore()).isEqualTo(85.0);
  }

  @Test
  void rejectsInsufficientOrZeroDispersionHistory() {
    RobustZScoreCalculator.Result insufficient =
        RobustZScoreCalculator.calculate(10, List.of(1d, 2d, 3d));
    RobustZScoreCalculator.Result zeroDispersion =
        RobustZScoreCalculator.calculate(10, java.util.Collections.nCopies(20, 10d));

    assertThat(insufficient.status())
        .isEqualTo(RobustZScoreCalculator.Status.INSUFFICIENT_HISTORY);
    assertThat(zeroDispersion.status()).isEqualTo(RobustZScoreCalculator.Status.INVALID);
  }

  private static org.assertj.core.data.Offset<Double> within(double value) {
    return org.assertj.core.data.Offset.offset(value);
  }
}
