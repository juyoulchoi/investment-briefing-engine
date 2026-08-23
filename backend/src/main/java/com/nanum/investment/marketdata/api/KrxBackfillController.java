package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.KrxBackfillService;
import com.nanum.investment.marketdata.infrastructure.KrxBackfillRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/krx/backfill-jobs")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "KRX 데이터",
    description = "KRX 원본 데이터, 날짜별 Job 및 기간 백필 API")
public class KrxBackfillController {
  private final KrxBackfillService backfills;

  public KrxBackfillController(KrxBackfillService backfills) {
    this.backfills = backfills;
  }

  @PostMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX 기간 백필 Job 생성")
  public ResponseEntity<KrxBackfillRepository.BackfillJobView> start(
      @Valid @RequestBody StartBackfillRequest request) {
    return ResponseEntity.accepted()
        .body(
            backfills.start(
                request.from(), request.to(), request.datasets(), request.requestIntervalMillis()));
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX 기간 백필 Job 목록 조회")
  public List<KrxBackfillRepository.BackfillJobView> findAll(
      @RequestParam(defaultValue = "20") int limit) {
    return backfills.findAll(limit);
  }

  @GetMapping("/{jobId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX 기간 백필 Job 및 날짜별 진행상태 조회")
  public KrxBackfillRepository.BackfillJobView find(@PathVariable UUID jobId) {
    return backfills.find(jobId);
  }

  @PostMapping("/{jobId}/pause")
  @io.swagger.v3.oas.annotations.Operation(summary = "실행 중인 KRX 기간 백필 Job 일시중지")
  public ResponseEntity<KrxBackfillRepository.BackfillJobView> pause(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(backfills.pause(jobId));
  }

  @PostMapping("/{jobId}/resume")
  @io.swagger.v3.oas.annotations.Operation(summary = "일시중지한 KRX 기간 백필 Job 이어서 실행")
  public ResponseEntity<KrxBackfillRepository.BackfillJobView> resume(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(backfills.resume(jobId));
  }

  @PostMapping("/{jobId}/retry-failures")
  @io.swagger.v3.oas.annotations.Operation(summary = "실패 날짜 전체 또는 실패 Dataset만 재실행")
  public ResponseEntity<KrxBackfillRepository.BackfillJobView> retryFailures(
      @PathVariable UUID jobId,
      @RequestParam(defaultValue = "DATASET") KrxBackfillService.RetryScope scope) {
    return ResponseEntity.accepted().body(backfills.retryFailures(jobId, scope));
  }

  @PostMapping("/{jobId}/cancel")
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX 기간 백필 Job 취소")
  public ResponseEntity<KrxBackfillRepository.BackfillJobView> cancel(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(backfills.cancel(jobId));
  }

  public record StartBackfillRequest(
      @NotNull LocalDate from,
      @NotNull LocalDate to,
      List<String> datasets,
      @Min(0) @Max(60000) Long requestIntervalMillis) {
    public StartBackfillRequest {
      if (requestIntervalMillis == null) requestIntervalMillis = 250L;
    }
  }
}
