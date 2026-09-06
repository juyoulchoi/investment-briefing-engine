package com.nanum.investment.briefing.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts a market metric to an outlier-resistant standardized score. */
public final class RobustZScoreCalculator {
  static final int MINIMUM_SAMPLE_COUNT = 20;
  private static final double NORMALIZED_MAD_FACTOR = 0.6744897501960817;
  private static final double Z_SCORE_LIMIT = 3.5;

  private RobustZScoreCalculator() {}

  public static Result calculate(double currentValue, List<Double> history) {
    if (!Double.isFinite(currentValue)) {
      return Result.invalid("현재 값이 유한한 숫자가 아닙니다.", history == null ? 0 : history.size());
    }
    if (history == null || history.size() < MINIMUM_SAMPLE_COUNT) {
      return Result.insufficient(history == null ? 0 : history.size());
    }

    List<Double> values = new ArrayList<>(history.size());
    for (Double value : history) {
      if (value == null || !Double.isFinite(value)) {
        return Result.invalid("이력에 유효하지 않은 값이 포함되어 있습니다.", history.size());
      }
      values.add(value);
    }

    double median = median(values);
    List<Double> deviations = new ArrayList<>(values.size());
    for (double value : values) {
      deviations.add(Math.abs(value - median));
    }
    double mad = median(deviations);
    if (mad == 0) {
      return Result.invalid("중앙절대편차가 0이어서 표준화할 수 없습니다.", values.size());
    }

    double rawZScore = NORMALIZED_MAD_FACTOR * (currentValue - median) / mad;
    double cappedZScore = Math.max(-Z_SCORE_LIMIT, Math.min(Z_SCORE_LIMIT, rawZScore));
    double normalizedScore = Math.max(0, Math.min(100, 50 + cappedZScore * 10));
    return new Result(
        Status.AVAILABLE,
        values.size(),
        median,
        mad,
        rawZScore,
        cappedZScore,
        normalizedScore,
        null);
  }

  private static double median(List<Double> source) {
    List<Double> sorted = new ArrayList<>(source);
    Collections.sort(sorted);
    int middle = sorted.size() / 2;
    if (sorted.size() % 2 == 1) {
      return sorted.get(middle);
    }
    return (sorted.get(middle - 1) + sorted.get(middle)) / 2;
  }

  public enum Status {
    AVAILABLE,
    INSUFFICIENT_HISTORY,
    INVALID
  }

  public record Result(
      Status status,
      int sampleCount,
      Double median,
      Double medianAbsoluteDeviation,
      Double rawZScore,
      Double cappedZScore,
      Double normalizedScore,
      String reason) {

    private static Result insufficient(int sampleCount) {
      return new Result(
          Status.INSUFFICIENT_HISTORY,
          sampleCount,
          null,
          null,
          null,
          null,
          null,
          "최소 " + MINIMUM_SAMPLE_COUNT + "개 이력이 필요합니다.");
    }

    private static Result invalid(String reason, int sampleCount) {
      return new Result(Status.INVALID, sampleCount, null, null, null, null, null, reason);
    }
  }
}
