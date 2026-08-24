package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.FredMacroService;
import com.nanum.investment.marketdata.infrastructure.FredMacroRepository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fred")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "FRED 데이터",
    description = "FRED Macro Series 기준정보, 관측값, 정정이력 및 기간 수집 Job API")
public class FredMacroController {
  private final FredMacroService service;

  public FredMacroController(FredMacroService service) {
    this.service = service;
  }

  @GetMapping("/series")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 목록 조회")
  public List<SeriesView> series(@RequestParam(defaultValue = "false") boolean activeOnly) {
    return service.series(activeOnly);
  }

  @GetMapping("/series/{seriesCode}")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 상세 조회")
  public SeriesView series(@PathVariable String seriesCode) {
    return service.findSeries(seriesCode);
  }

  @PostMapping("/series")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 등록")
  public SeriesView register(@Valid @RequestBody SeriesRequest request) {
    return service.register(request.seriesCode(), request.command());
  }

  @PutMapping("/series/{seriesCode}")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 설정 수정 및 메타데이터 동기화")
  public SeriesView update(
      @PathVariable String seriesCode, @Valid @RequestBody SeriesSettings request) {
    return service.register(seriesCode, request.command());
  }

  @PostMapping("/series/{seriesCode}/refresh-metadata")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Series 원본 메타데이터 갱신")
  public SeriesView refreshMetadata(@PathVariable String seriesCode) {
    return service.refreshMetadata(seriesCode);
  }

  @PostMapping("/series/{seriesCode}/activate")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 활성화")
  public SeriesView activate(@PathVariable String seriesCode) {
    return service.setActive(seriesCode, true);
  }

  @PostMapping("/series/{seriesCode}/deactivate")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 비활성화")
  public SeriesView deactivate(@PathVariable String seriesCode) {
    return service.setActive(seriesCode, false);
  }

  @GetMapping("/series/{seriesCode}/observations")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 기간 관측값 조회")
  public List<Map<String, Object>> observations(
      @PathVariable String seriesCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.observations(seriesCode, from, to);
  }

  @GetMapping("/series/{seriesCode}/revisions")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 기간 정정이력 조회")
  public List<Map<String, Object>> revisions(
      @PathVariable String seriesCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.revisions(seriesCode, from, to);
  }

  @PostMapping("/collection-jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 기간 수집 Job 생성")
  public ResponseEntity<JobView> start(@Valid @RequestBody StartJobRequest request) {
    return ResponseEntity.accepted()
        .body(
            service.startJob(
                request.from(),
                request.to(),
                request.seriesCodes(),
                request.requestIntervalMillis()));
  }

  @GetMapping("/collection-jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 수집 Job 목록 조회")
  public List<JobView> jobs(@RequestParam(defaultValue = "20") int limit) {
    return service.jobs(limit);
  }

  @GetMapping("/collection-jobs/{jobId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 수집 Job 및 Series별 상태 조회")
  public JobView job(@PathVariable UUID jobId) {
    return service.job(jobId);
  }

  @PostMapping("/collection-jobs/{jobId}/pause")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 수집 Job 일시중지")
  public ResponseEntity<JobView> pause(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(service.pause(jobId));
  }

  @PostMapping("/collection-jobs/{jobId}/resume")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 수집 Job 이어서 실행")
  public ResponseEntity<JobView> resume(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(service.resume(jobId));
  }

  @PostMapping("/collection-jobs/{jobId}/retry-failures")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 수집 Job 실패 Series 재실행")
  public ResponseEntity<JobView> retry(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(service.retryFailures(jobId));
  }

  @PostMapping("/collection-jobs/{jobId}/cancel")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED Macro Series 수집 Job 취소")
  public ResponseEntity<JobView> cancel(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(service.cancel(jobId));
  }

  public record SeriesRequest(
      @NotBlank String seriesCode,
      String displayName,
      String categoryCode,
      String countryCode,
      String transformCode,
      String aggregationCode,
      String collectionCycleCode,
      @Min(0) @Max(3650) Integer refreshOverlapDays,
      String vintagePolicyCode,
      String targetCode,
      String useYn) {
    SeriesCommand command() {
      return new SeriesCommand(
          displayName,
          categoryCode,
          countryCode,
          transformCode,
          aggregationCode,
          collectionCycleCode,
          refreshOverlapDays,
          vintagePolicyCode,
          targetCode,
          useYn);
    }
  }

  public record SeriesSettings(
      String displayName,
      String categoryCode,
      String countryCode,
      String transformCode,
      String aggregationCode,
      String collectionCycleCode,
      @Min(0) @Max(3650) Integer refreshOverlapDays,
      String vintagePolicyCode,
      String targetCode,
      String useYn) {
    SeriesCommand command() {
      return new SeriesCommand(
          displayName,
          categoryCode,
          countryCode,
          transformCode,
          aggregationCode,
          collectionCycleCode,
          refreshOverlapDays,
          vintagePolicyCode,
          targetCode,
          useYn);
    }
  }

  public record StartJobRequest(
      @NotNull LocalDate from,
      @NotNull LocalDate to,
      List<String> seriesCodes,
      @Min(0) @Max(60000) Long requestIntervalMillis) {
    public StartJobRequest {
      if (requestIntervalMillis == null) requestIntervalMillis = 250L;
    }
  }
}
