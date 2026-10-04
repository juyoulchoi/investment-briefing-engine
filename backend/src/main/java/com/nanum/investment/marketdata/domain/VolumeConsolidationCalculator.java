package com.nanum.investment.marketdata.domain;

import static com.nanum.investment.marketdata.domain.VolumeConsolidation.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VolumeConsolidationCalculator {
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final MathContext MC = MathContext.DECIMAL128;
  private final Rules rules = Rules.defaults();

  public Candidate evaluate(List<LocalDate> dates, List<Bar> input, Master master) {
    if (dates.isEmpty() || input.isEmpty()) throw new IllegalArgumentException("시세가 필요합니다.");
    var reasons = new ArrayList<String>();
    var checks =
        new ArrayList<>(
            List.of("관리·투자주의·정리매매·거래정지 상태 최종 확인", "수정주가 및 기업행사 확인", "외국인·기관 수급 확인", "공시 확인"));
    Bar last = input.stream().max(java.util.Comparator.comparing(Bar::date)).orElseThrow();
    LocalDate base = dates.getLast();
    if (master == null || !"보통주".equals(master.kind()) || !"주권".equals(master.securityGroup())) {
      reasons.add(master == null ? "보통주 분류 확인 불가" : "보통주 외 종목");
    }
    if (master != null && master.date().isBefore(base)) checks.add("기본정보가 기준일보다 과거 자료");
    String category = (last.name() + " " + last.section()).toUpperCase(java.util.Locale.ROOT);
    if (category.contains("스팩") || category.contains("SPAC")) reasons.add("스팩");
    if (category.contains("관리")
        || category.contains("주의")
        || category.contains("경고")
        || category.contains("위험")
        || category.contains("정리매매")) reasons.add("위험 지정 종목");

    Map<LocalDate, Bar> byDate = new HashMap<>();
    for (Bar bar : input) {
      if (byDate.put(bar.date(), bar) != null) reasons.add("일별 시세 중복");
    }
    List<Bar> bars = dates.stream().map(byDate::get).toList();
    if (dates.size() != 75 || bars.stream().anyMatch(java.util.Objects::isNull)) {
      reasons.add("75거래일 시세 부족 또는 누락");
      return result(last, base, master, reasons, checks, null);
    }
    if (bars.stream().anyMatch(b -> !valid(b))) {
      reasons.add("시세·거래량·거래대금 오류 또는 거래 없음");
      return result(last, base, master, reasons, checks, null);
    }
    if (bars.stream().anyMatch(b -> b.listedShares() == null || b.listedShares().signum() <= 0)) {
      reasons.add("상장주식수 확인 불가");
    } else if (bars.stream()
            .map(Bar::listedShares)
            .map(BigDecimal::stripTrailingZeros)
            .distinct()
            .count()
        > 1) {
      reasons.add("상장주식수 변동: 기업행사 검토 필요");
    }
    List<Bar> recent = bars.subList(60, 75);
    BigDecimal recentVolume = average(recent.stream().map(Bar::volume).toList());
    BigDecimal baselineVolume = average(bars.subList(0, 60).stream().map(Bar::volume).toList());
    BigDecimal multiple = divide(recentVolume, baselineVolume);
    BigDecimal ret = pct(bars.get(74).close(), bars.get(59).close());
    BigDecimal range =
        pct(
            max(recent.stream().map(Bar::close).toList()),
            min(recent.stream().map(Bar::close).toList()));
    BigDecimal value = average(recent.stream().map(Bar::value).toList());
    int spike = 0, sustained = 0;
    BigDecimal maxMultiple = BigDecimal.ZERO;
    for (int i = 60; i < 75; i++) {
      BigDecimal daily =
          divide(
              bars.get(i).volume(),
              average(bars.subList(i - 20, i).stream().map(Bar::volume).toList()));
      if (daily.compareTo(rules.spikeMultiple()) >= 0) spike++;
      if (daily.compareTo(rules.sustainedMultiple()) >= 0) sustained++;
      maxMultiple = maxMultiple.max(daily);
    }
    if (ret.compareTo(rules.minReturnPct()) < 0 || ret.compareTo(rules.maxReturnPct()) > 0)
      reasons.add("3주 수익률 범위 이탈");
    if (range.compareTo(rules.maxRangePct()) > 0) reasons.add("종가 횡보 폭 초과");
    if (multiple.compareTo(rules.minVolumeMultiple()) < 0) reasons.add("평균 거래량 2배 미만");
    if (spike < rules.minSpikeDays()) reasons.add("거래량 5배 급증일 없음");
    if (sustained < rules.minSustainedDays()) reasons.add("거래량 2배 지속일 3일 미만");
    if (value.compareTo(rules.minAverageValue()) < 0) reasons.add("평균 거래대금 20억원 미만");
    BigDecimal ma20 = average(bars.subList(55, 75).stream().map(Bar::close).toList());
    Metrics metrics =
        new Metrics(
            ret,
            range,
            multiple,
            multiple.subtract(BigDecimal.ONE).multiply(HUNDRED),
            spike,
            sustained,
            maxMultiple,
            value,
            max(recent.stream().map(Bar::high).toList()),
            min(recent.stream().map(Bar::low).toList()),
            last.close(),
            ma20,
            last.close().compareTo(ma20) >= 0,
            pct(last.close(), bars.get(14).close()),
            pct(last.close(), max(bars.subList(15, 75).stream().map(Bar::high).toList())));
    return result(last, base, master, reasons, checks, metrics);
  }

  private Candidate result(
      Bar last,
      LocalDate base,
      Master master,
      List<String> reasons,
      List<String> checks,
      Metrics metrics) {
    return new Candidate(
        last.market(),
        last.code(),
        last.name(),
        base,
        master == null ? null : master.date(),
        reasons.isEmpty(),
        reasons.isEmpty() ? "REQUIRES_VERIFICATION" : "EXCLUDED",
        List.copyOf(reasons),
        List.copyOf(checks),
        metrics,
        null,
        null,
        "UNAVAILABLE",
        "NOT_CHECKED");
  }

  /** The selection-day box stays fixed. Data is supplied only through the evaluation cutoff. */
  public Tracking track(Candidate selected, List<LocalDate> futureDates, List<Bar> history) {
    if (!selected.quantitativeMatch())
      throw new IllegalArgumentException("수치 조건을 통과한 후보만 추적할 수 있습니다.");
    BigDecimal high = selected.metrics().boxHigh(), low = selected.metrics().boxLow();
    var events = new ArrayList<Event>();
    var warnings = new ArrayList<>(selected.checksRequired());
    warnings.add("종가 신호는 다음 거래일 이후 체결을 가정합니다. 주문이나 수익률 검증 결과가 아닙니다.");
    Map<LocalDate, Bar> byDate = new HashMap<>();
    history.forEach(b -> byDate.put(b.date(), b));
    var prior =
        new ArrayList<>(
            history.stream()
                .filter(b -> !b.date().isAfter(selected.baseDate()))
                .sorted(java.util.Comparator.comparing(Bar::date))
                .toList());
    int breakoutIndex = -1;
    String state = "WATCHING";
    LocalDate through = selected.baseDate();
    BigDecimal shares = prior.isEmpty() ? null : prior.getLast().listedShares();
    for (int i = 0; i < futureDates.size(); i++) {
      LocalDate date = futureDates.get(i);
      Bar b = byDate.get(date);
      if (b == null
          || !valid(b)
          || shares == null
          || b.listedShares() == null
          || b.listedShares().compareTo(shares) != 0
          || prior.size() < 20) {
        state = "DATA_UNAVAILABLE";
        warnings.add("추적 구간 시세 누락·거래 없음·상장주식수 변동으로 판정 중단");
        break;
      }
      BigDecimal volume =
          divide(
              b.volume(),
              average(
                  prior.subList(prior.size() - 20, prior.size()).stream()
                      .map(Bar::volume)
                      .toList()));
      through = date;
      if (breakoutIndex < 0) {
        if (b.close().compareTo(high) > 0
            && volume.compareTo(rules.breakoutVolumeMultiple()) >= 0) {
          breakoutIndex = i;
          state = "BREAKOUT";
          events.add(new Event(date, state, b.close(), volume));
        } else if (i + 1 >= rules.breakoutDeadlineDays()) {
          state = "EXPIRED";
          events.add(new Event(date, state, b.close(), volume));
          break;
        }
      } else if (b.close().compareTo(high) < 0) {
        state = "FAILED";
        events.add(new Event(date, state, b.close(), volume));
        break;
      } else if (!state.equals("RETEST_HELD")
          && i - breakoutIndex <= rules.retestDeadlineDays()
          && b.low().compareTo(high.multiply(new BigDecimal("0.99"))) >= 0
          && b.low().compareTo(high.multiply(new BigDecimal("1.01"))) <= 0) {
        state = "RETEST_HELD";
        events.add(new Event(date, state, b.close(), volume));
      }
      prior.add(b);
    }
    return new Tracking(
        selected.stockCode(),
        selected.baseDate(),
        through,
        high,
        low,
        state,
        List.copyOf(events),
        List.copyOf(warnings));
  }

  private static boolean valid(Bar b) {
    return positive(b.open())
        && positive(b.high())
        && positive(b.low())
        && positive(b.close())
        && positive(b.volume())
        && positive(b.value())
        && b.low().compareTo(b.high()) <= 0
        && b.close().compareTo(b.low()) >= 0
        && b.close().compareTo(b.high()) <= 0
        && b.open().compareTo(b.low()) >= 0
        && b.open().compareTo(b.high()) <= 0;
  }

  private static boolean positive(BigDecimal v) {
    return v != null && v.signum() > 0;
  }

  private static BigDecimal divide(BigDecimal a, BigDecimal b) {
    return a.divide(b, MC);
  }

  private static BigDecimal pct(BigDecimal a, BigDecimal b) {
    return divide(a.subtract(b).multiply(HUNDRED), b);
  }

  private static BigDecimal average(List<BigDecimal> values) {
    return divide(
        values.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
        BigDecimal.valueOf(values.size()));
  }

  private static BigDecimal min(List<BigDecimal> values) {
    return values.stream().min(BigDecimal::compareTo).orElseThrow();
  }

  private static BigDecimal max(List<BigDecimal> values) {
    return values.stream().max(BigDecimal::compareTo).orElseThrow();
  }
}
