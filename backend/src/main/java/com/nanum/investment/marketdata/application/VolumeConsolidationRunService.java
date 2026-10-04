package com.nanum.investment.marketdata.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.marketdata.domain.VolumeConsolidation.*;
import com.nanum.investment.marketdata.domain.VolumeConsolidationRun;
import com.nanum.investment.marketdata.infrastructure.VolumeConsolidationRunRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VolumeConsolidationRunService {
  private final VolumeConsolidationService screens;
  private final VolumeConsolidationRunRepository runs;
  private final ObjectMapper json;
  private final java.time.OffsetDateTime applicationStartedAt = java.time.OffsetDateTime.now();

  public VolumeConsolidationRunService(
      VolumeConsolidationService screens,
      VolumeConsolidationRunRepository runs,
      ObjectMapper json) {
    this.screens = screens;
    this.runs = runs;
    this.json = json;
  }

  /** Collection failures and scan failures are auditable, but must not abort the briefing. */
  public void afterCollection(LocalDate collectedDate) {
    try {
      execute(collectedDate, "AFTER_KRX_COLLECTION");
    } catch (RuntimeException error) {
      log.error("KRX 수집 후 거래량·횡보 검색 실행/이력 기록 실패. collectedDate={}", collectedDate, error);
    }
  }

  public VolumeConsolidationRun runManually(LocalDate date) {
    LocalDate requested = date == null ? LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1) : date;
    if (!requested.isBefore(LocalDate.now(ZoneId.of("Asia/Seoul"))))
      throw new BusinessException(ErrorCode.INVALID_REQUEST, "오늘 이전의 종가 기준일만 검색할 수 있습니다.");
    return execute(requested, "MANUAL");
  }

  private VolumeConsolidationRun execute(LocalDate date, String trigger) {
    UUID id = UUID.randomUUID();
    runs.start(id, trigger, date, Rules.defaults().version());
    try {
      if (date == null || !date.isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
        runs.finish(
            id,
            "SKIPPED",
            date,
            null,
            null,
            date == null ? "국내 시세 수집 미완료로 검색 생략" : "당일 종가 확정 전 검색 생략: 다음 아침 수집 후 검색");
      } else {
        Screen result = screens.screen(date);
        boolean partial =
            result.availableDays() < 75
                || !"NO_UNEXPLAINED_GAPS".equals(result.dateCoverageStatus())
                || result.coverage().stream()
                    .anyMatch(c -> !Objects.equals(c.latestDate(), result.baseDate()));
        String status = partial ? "PARTIAL" : "CALCULATED";
        runs.finish(
            id,
            status,
            result.baseDate(),
            result.matchCount(),
            json.writeValueAsString(result),
            null);
        log.info(
            "거래량·횡보 후보 검색 저장. runId={}, trigger={}, baseDate={}, status={}, universe={}, matches={}",
            id,
            trigger,
            result.baseDate(),
            status,
            result.universeCount(),
            result.matchCount());
      }
    } catch (Exception error) {
      runs.finish(
          id, "FAILED", date, null, null, "검색 계산 또는 결과 저장 실패: " + error.getClass().getSimpleName());
      log.error("거래량·횡보 검색 실패. runId={}, baseDate={}", id, date, error);
    }
    return runs.find(id).orElseThrow();
  }

  public List<VolumeConsolidationRun> latest(int limit) {
    return runs.latest(limit);
  }

  @org.springframework.context.event.EventListener(
      org.springframework.boot.context.event.ApplicationReadyEvent.class)
  public void recoverInterrupted() {
    int recovered = runs.recoverInterrupted(applicationStartedAt);
    if (recovered > 0) log.warn("재시작 이전 미완료 후보 검색을 실패 이력으로 정리했습니다. count={}", recovered);
  }

  public Screen result(UUID id) {
    String payload =
        runs.result(id)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "저장된 검색 결과가 없습니다."));
    try {
      return json.readValue(payload, Screen.class);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("저장된 검색 결과를 읽을 수 없습니다.", error);
    }
  }
}
