package com.nanum.investment.marketdata.application;

import static com.nanum.investment.marketdata.domain.VolumeConsolidation.*;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.marketdata.domain.VolumeConsolidationCalculator;
import com.nanum.investment.marketdata.infrastructure.VolumeConsolidationRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class VolumeConsolidationService {
  private final VolumeConsolidationRepository repository;
  private final VolumeConsolidationCalculator calculator = new VolumeConsolidationCalculator();

  public VolumeConsolidationService(VolumeConsolidationRepository repository) {
    this.repository = repository;
  }

  public Screen screen(LocalDate requested) {
    LocalDate cutoff = cutoff(requested);
    var coverage = repository.coverage(cutoff);
    // Default also excludes today's rows: the ingestion store has no official EOD finalization
    // flag.
    List<LocalDate> dates = repository.dates(cutoff, 75);
    var warnings =
        new ArrayList<>(
            List.of(
                "수치 조건 통과는 매집 확인이나 매수 추천이 아닙니다. 기준값의 승률은 검증되지 않았습니다.",
                "저장된 시세·지수·개장 달력의 날짜를 사용합니다. 모든 소스에 빠진 거래일은 감지하지 못할 수 있습니다.",
                "원본 주가는 수정주가가 아닙니다. 상장주식수 변동 종목은 제외하지만 모든 기업행사를 감지하지는 못합니다.",
                "수급 데이터와 공시 연결이 없어 확인 불가로 표시합니다. 위험 지정 전체의 해제 여부도 별도 확인해야 합니다.",
                "과거 조회는 현재 저장된 정정 데이터로 재계산한 결과이며 당시 이용 가능했던 정보의 백테스트가 아닙니다."));
    LocalDate base = dates.isEmpty() ? null : dates.getLast();
    if (base == null || dates.size() < 75) warnings.add("75거래일 데이터가 부족합니다.");
    for (Coverage c : coverage) {
      if (c.latestDate() == null || !c.latestDate().equals(base))
        warnings.add(c.market() + " 시세가 기준일까지 수집되지 않았습니다.");
      if (c.masterDate() == null || !c.masterDate().equals(base))
        warnings.add(c.market() + " 기본정보 최신일: " + c.masterDate());
    }
    if (base == null)
      return new Screen(
          cutoff,
          null,
          null,
          null,
          null,
          0,
          "NO_DATA",
          List.of(),
          Rules.defaults(),
          coverage,
          0,
          0,
          Map.of(),
          warnings,
          List.of());
    var gaps = repository.unverifiedWeekdays(dates.getFirst(), cutoff);
    if (!gaps.isEmpty())
      warnings.add(
          "휴장·누락 여부를 확인할 수 없는 평일이 있습니다. 15일·60일 수치는 저장일 기준 잠정 계산이며 실제 최근 3주 검색 결과로 확정할 수 없습니다. 해당 구간의 돌파 추적은 차단됩니다.");
    Map<String, Master> masters = repository.masters(base);
    Map<String, List<Bar>> grouped =
        repository.bars(dates.getFirst(), base, null, null).stream()
            .collect(Collectors.groupingBy(b -> b.market() + ":" + b.code()));
    var rows = new ArrayList<Candidate>();
    grouped.forEach((key, bars) -> rows.add(calculator.evaluate(dates, bars, masters.get(key))));
    rows.sort(
        Comparator.comparing(Candidate::quantitativeMatch)
            .reversed()
            .thenComparing(
                (Candidate c) -> c.metrics() == null ? -1 : c.metrics().sustainedDays(),
                Comparator.reverseOrder())
            .thenComparing(
                c -> c.metrics() == null ? null : c.metrics().volumeMultiple(),
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(
                c -> c.metrics() == null ? null : c.metrics().closeRangePct(),
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Candidate::market)
            .thenComparing(Candidate::stockCode));
    Map<String, Long> exclusions =
        rows.stream()
            .flatMap(r -> r.exclusionReasons().stream())
            .collect(Collectors.groupingBy(s -> s, TreeMap::new, Collectors.counting()));
    return new Screen(
        cutoff,
        base,
        dates.size() >= 15 ? dates.get(dates.size() - 15) : null,
        dates.size() == 75 ? dates.getFirst() : null,
        dates.size() > 15 ? dates.get(dates.size() - 16) : null,
        dates.size(),
        gaps.isEmpty() ? "NO_UNEXPLAINED_GAPS" : "DATE_GAPS_UNVERIFIED",
        gaps,
        Rules.defaults(),
        coverage,
        rows.size(),
        (int) rows.stream().filter(Candidate::quantitativeMatch).count(),
        exclusions,
        warnings,
        List.copyOf(rows));
  }

  public Tracking track(String market, String stockCode, LocalDate selectionDate, LocalDate asOf) {
    if (!List.of("KOSPI", "KOSDAQ").contains(market)
        || stockCode == null
        || !stockCode.matches("[0-9A-Z]{6}")) throw invalid("시장 또는 종목코드가 올바르지 않습니다.");
    LocalDate end = cutoff(asOf);
    if (selectionDate == null
        || selectionDate.isAfter(end)
        || selectionDate.isBefore(end.minusYears(1))) throw invalid("선정일은 조회일 이전 1년 이내여야 합니다.");
    var dates = repository.dates(selectionDate, 75);
    if (dates.isEmpty() || !dates.getLast().equals(selectionDate))
      throw invalid("선정일에 저장된 거래일 데이터가 없습니다.");
    if (!repository.unverifiedWeekdays(dates.getFirst(), end).isEmpty())
      throw invalid("휴장·수집 누락 여부가 미확인인 평일이 있어 돌파 추적을 판정할 수 없습니다. 시장 달력 및 시세를 먼저 확인해 주세요.");
    var bars = repository.bars(dates.getFirst(), end, stockCode, market);
    var selectionBars = bars.stream().filter(b -> !b.date().isAfter(selectionDate)).toList();
    if (selectionBars.isEmpty()) throw invalid("선정일 종목 시세가 없습니다.");
    Candidate candidate =
        calculator.evaluate(
            dates, selectionBars, repository.masters(selectionDate).get(market + ":" + stockCode));
    if (!candidate.quantitativeMatch())
      throw invalid("선정일 수치 조건을 통과한 후보가 아닙니다: " + String.join(", ", candidate.exclusionReasons()));
    var future = repository.dates(end, 400).stream().filter(d -> d.isAfter(selectionDate)).toList();
    return calculator.track(candidate, future, bars);
  }

  private LocalDate cutoff(LocalDate date) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    if (date != null && !date.isBefore(today))
      throw invalid("종가 확정 여부를 확인할 수 없어 오늘 이후 날짜는 조회할 수 없습니다.");
    return date == null ? today.minusDays(1) : date;
  }

  private BusinessException invalid(String message) {
    return new BusinessException(ErrorCode.INVALID_REQUEST, message);
  }
}
