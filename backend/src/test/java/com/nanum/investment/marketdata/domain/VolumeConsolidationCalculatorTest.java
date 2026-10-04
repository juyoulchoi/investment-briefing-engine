package com.nanum.investment.marketdata.domain;

import static com.nanum.investment.marketdata.domain.VolumeConsolidation.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VolumeConsolidationCalculatorTest {
  private final VolumeConsolidationCalculator calculator = new VolumeConsolidationCalculator();
  private final LocalDate start = LocalDate.of(2025, 1, 1);

  private List<LocalDate> dates() {
    return java.util.stream.IntStream.range(0, 75).mapToObj(start::plusDays).toList();
  }

  private Bar bar(int day, String close, String high, String low, String volume) {
    return new Bar(
        start.plusDays(day),
        "KOSPI",
        "005930",
        "테스트",
        "",
        new BigDecimal(close),
        new BigDecimal(high),
        new BigDecimal(low),
        new BigDecimal(close),
        new BigDecimal(volume),
        new BigDecimal("3000000000"),
        new BigDecimal("100000000"));
  }

  private List<Bar> bars() {
    var rows = new ArrayList<Bar>();
    for (int i = 0; i < 75; i++) rows.add(bar(i, "100", "101", "99", i < 60 ? "100" : "1000"));
    return rows;
  }

  private Candidate evaluate(List<Bar> rows) {
    return calculator.evaluate(dates(), rows, new Master(start.plusDays(74), "보통주", "주권"));
  }

  @Test
  void usesNonOverlappingBaselineAndPreviousTwentyDaysExcludingSpikeDay() {
    Candidate c = evaluate(bars());
    assertThat(c.quantitativeMatch()).isTrue();
    assertThat(c.verificationStatus()).isEqualTo("REQUIRES_VERIFICATION");
    assertThat(c.metrics().volumeMultiple()).isEqualByComparingTo("10");
    assertThat(c.metrics().volumeIncreasePct()).isEqualByComparingTo("900");
    assertThat(c.metrics().spikeDays()).isEqualTo(3);
    assertThat(c.metrics().sustainedDays()).isEqualTo(9);
    assertThat(c.foreignNetAmount()).isNull();
    assertThat(c.flowStatus()).isEqualTo("UNAVAILABLE");
  }

  @Test
  void smallEndpointReturnDoesNotHideWideIntermediateRange() {
    var rows = bars();
    rows.set(65, bar(65, "130", "131", "99", "1000"));
    Candidate c = evaluate(rows);
    assertThat(c.metrics().returnPct()).isEqualByComparingTo("0");
    assertThat(c.exclusionReasons()).contains("종가 횡보 폭 초과");
  }

  @Test
  void includesExactlyFivePercentButRejectsBeyondBoundaryAndLargeNegativeReturns() {
    var rows = bars();
    rows.set(74, bar(74, "105", "106", "99", "1000"));
    assertThat(evaluate(rows).quantitativeMatch()).isTrue();
    rows.set(74, bar(74, "105.01", "106", "99", "1000"));
    assertThat(evaluate(rows).exclusionReasons()).contains("3주 수익률 범위 이탈");
    rows.set(74, bar(74, "90", "101", "89", "1000"));
    assertThat(evaluate(rows).exclusionReasons()).contains("3주 수익률 범위 이탈");
  }

  @Test
  void missingSessionAndZeroVolumeAreNotSilentlyCompressedOrFilled() {
    var rows = bars();
    rows.remove(20);
    assertThat(evaluate(rows).metrics()).isNull();
    assertThat(evaluate(rows).exclusionReasons()).contains("75거래일 시세 부족 또는 누락");
    rows = bars();
    rows.set(20, bar(20, "100", "101", "99", "0"));
    assertThat(evaluate(rows).exclusionReasons()).contains("시세·거래량·거래대금 오류 또는 거래 없음");
  }

  @Test
  void shareChangesAndUnknownOrPreferredShareClassAreExcluded() {
    var rows = bars();
    Bar b = rows.getLast();
    rows.set(
        74,
        new Bar(
            b.date(),
            b.market(),
            b.code(),
            b.name(),
            b.section(),
            b.open(),
            b.high(),
            b.low(),
            b.close(),
            b.volume(),
            b.value(),
            new BigDecimal("200000000")));
    assertThat(evaluate(rows).exclusionReasons()).contains("상장주식수 변동: 기업행사 검토 필요");
    assertThat(calculator.evaluate(dates(), bars(), null).exclusionReasons())
        .contains("보통주 분류 확인 불가");
    assertThat(
            calculator.evaluate(dates(), bars(), new Master(start, "우선주", "주권")).exclusionReasons())
        .contains("보통주 외 종목");
  }

  @Test
  void spacAndRiskDesignationAreExcluded() {
    var rows = bars();
    Bar b = rows.getLast();
    rows.set(
        74,
        new Bar(
            b.date(),
            b.market(),
            b.code(),
            "테스트스팩",
            "관리종목",
            b.open(),
            b.high(),
            b.low(),
            b.close(),
            b.volume(),
            b.value(),
            b.listedShares()));
    assertThat(evaluate(rows).exclusionReasons()).contains("스팩", "위험 지정 종목");
  }

  @Test
  void fixedBoxTracksBreakoutRetestAndFailureWithoutMovingResistance() {
    var history = bars();
    Candidate selected = evaluate(history);
    history.add(bar(75, "105", "110", "100", "3000"));
    history.add(bar(76, "102", "106", "101", "1000"));
    history.add(bar(77, "100", "104", "99", "1000"));
    Tracking t =
        calculator.track(
            selected, List.of(start.plusDays(75), start.plusDays(76), start.plusDays(77)), history);
    assertThat(t.boxHigh()).isEqualByComparingTo("101");
    assertThat(t.events())
        .extracting(Event::type)
        .containsExactly("BREAKOUT", "RETEST_HELD", "FAILED");
    assertThat(t.state()).isEqualTo("FAILED");
  }

  @Test
  void closeAtResistanceDoesNotBreakOutAndObservationExpiresAfterTenSessions() {
    var history = bars();
    var selected = evaluate(history);
    for (int i = 75; i < 86; i++) history.add(bar(i, i == 85 ? "110" : "101", "111", "99", "3000"));
    var future = java.util.stream.IntStream.range(75, 86).mapToObj(start::plusDays).toList();
    Tracking t = calculator.track(selected, future, history);
    assertThat(t.state()).isEqualTo("EXPIRED");
    assertThat(t.evaluatedThrough()).isEqualTo(start.plusDays(84));
    assertThat(t.events()).extracting(Event::type).containsExactly("EXPIRED");
  }

  @Test
  void retestIsOnlyRecognizedWithinFiveSessionsAfterBreakout() {
    var history = bars();
    var selected = evaluate(history);
    history.add(bar(75, "105", "110", "100", "3000"));
    for (int i = 76; i < 81; i++) history.add(bar(i, "108", "110", "105", "1000"));
    history.add(bar(81, "102", "106", "101", "1000"));
    Tracking t =
        calculator.track(
            selected,
            java.util.stream.IntStream.range(75, 82).mapToObj(start::plusDays).toList(),
            history);
    assertThat(t.state()).isEqualTo("BREAKOUT");
    assertThat(t.events()).hasSize(1);
  }

  @Test
  void missingFutureDataStopsSignalEvaluation() {
    var history = bars();
    var selected = evaluate(history);
    history.add(bar(76, "105", "110", "100", "3000"));
    var t = calculator.track(selected, List.of(start.plusDays(75), start.plusDays(76)), history);
    assertThat(t.state()).isEqualTo("DATA_UNAVAILABLE");
    assertThat(t.events()).isEmpty();
  }
}
