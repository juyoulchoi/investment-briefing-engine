package com.nanum.investment.marketdata.application;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.marketdata.domain.KrxDataset;
import com.nanum.investment.marketdata.infrastructure.KrxBackfillRepository;
import com.nanum.investment.marketdata.infrastructure.KrxCollectionJobRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class KrxBackfillService {
  private static final long MAX_RANGE_DAYS = 3660;
  private static final List<KrxDataset> DEFAULT_DATASETS =
      Arrays.stream(KrxDataset.values()).filter(value -> value.name().endsWith("_DAILY")).toList();

  private final KrxBackfillRepository backfills;
  private final KrxCollectionJobRepository dailyJobs;
  private final KrxBackfillJobRunner runner;

  public KrxBackfillService(
      KrxBackfillRepository backfills,
      KrxCollectionJobRepository dailyJobs,
      KrxBackfillJobRunner runner) {
    this.backfills = backfills;
    this.dailyJobs = dailyJobs;
    this.runner = runner;
  }

  public synchronized KrxBackfillRepository.BackfillJobView start(
      LocalDate from, LocalDate to, List<String> requestedDatasets, long requestIntervalMillis) {
    validatePeriod(from, to);
    if (requestIntervalMillis < 0 || requestIntervalMillis > 60000)
      throw invalid("requestIntervalMillis는 0~60000 범위여야 합니다.");
    List<KrxDataset> datasets = datasets(requestedDatasets);
    String datasetCodes =
        datasets.stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElseThrow();
    if (backfills.hasActiveOverlap(from, to, datasetCodes))
      throw new BusinessException(
          ErrorCode.DUPLICATE_RESOURCE, "동일 Dataset의 기간이 겹치는 활성 KRX 백필 Job이 있습니다.");

    List<KrxBackfillRepository.NewDay> days = new ArrayList<>();
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      Optional<Boolean> calendar = backfills.marketOpen(date);
      if (calendar.isPresent() && !calendar.get())
        days.add(new KrxBackfillRepository.NewDay(date, "SKIPPED", "KRX 시장 달력 휴장일"));
      else if (calendar.isEmpty()
          && (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY))
        days.add(new KrxBackfillRepository.NewDay(date, "SKIPPED", "주말"));
      else days.add(new KrxBackfillRepository.NewDay(date, "PENDING", null));
    }

    UUID id = UUID.randomUUID();
    backfills.create(id, from, to, datasetCodes, requestIntervalMillis, days);
    runner.run(id);
    return backfills.find(id);
  }

  public KrxBackfillRepository.BackfillJobView find(UUID id) {
    try {
      return backfills.find(id);
    } catch (NoSuchElementException exception) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, exception.getMessage());
    }
  }

  public List<KrxBackfillRepository.BackfillJobView> findAll(int limit) {
    return backfills.findAll(limit);
  }

  public KrxBackfillRepository.BackfillJobView pause(UUID id) {
    find(id);
    backfills.requestPause(id);
    return find(id);
  }

  public KrxBackfillRepository.BackfillJobView resume(UUID id) {
    find(id);
    backfills.queueResume(id);
    runner.run(id);
    return find(id);
  }

  public KrxBackfillRepository.BackfillJobView cancel(UUID id) {
    find(id);
    backfills.requestCancel(id);
    return find(id);
  }

  public KrxBackfillRepository.BackfillJobView retryFailures(UUID id, RetryScope scope) {
    var job = find(id);
    if (!List.of("COMPLETED_WITH_ERRORS", "FAILED").contains(job.status()))
      throw invalid("COMPLETED_WITH_ERRORS 또는 FAILED 상태에서만 실패 항목을 재처리할 수 있습니다.");
    int count = backfills.resetFailures(id, scope == RetryScope.DATASET, dailyJobs);
    if (count == 0) throw invalid("재처리할 실패 날짜가 없습니다.");
    runner.run(id);
    return find(id);
  }

  private List<KrxDataset> datasets(List<String> requested) {
    if (requested == null || requested.isEmpty()) return DEFAULT_DATASETS;
    try {
      return requested.stream()
          .map(String::trim)
          .map(String::toUpperCase)
          .map(KrxDataset::valueOf)
          .distinct()
          .toList();
    } catch (IllegalArgumentException exception) {
      throw invalid("지원하지 않는 KRX Dataset이 포함되어 있습니다.");
    }
  }

  private void validatePeriod(LocalDate from, LocalDate to) {
    if (from == null || to == null) throw invalid("from과 to가 필요합니다.");
    if (from.isAfter(to)) throw invalid("from은 to보다 늦을 수 없습니다.");
    if (to.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) throw invalid("to는 현재 날짜보다 늦을 수 없습니다.");
    if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS)
      throw invalid("한 번의 백필 기간은 10년을 초과할 수 없습니다.");
  }

  private BusinessException invalid(String message) {
    return new BusinessException(ErrorCode.INVALID_REQUEST, message);
  }

  public enum RetryScope {
    DATE,
    DATASET
  }
}
