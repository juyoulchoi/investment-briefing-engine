package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.KofiaCollectionService;
import com.nanum.investment.marketdata.application.KofiaCollectionService.CollectionView;
import com.nanum.investment.marketdata.application.KofiaCollectionService.DatasetView;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import com.nanum.investment.marketdata.infrastructure.KofiaRepository.JobView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kofia")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "KOFIA 데이터",
    description = "KOFIA FreeSIS Dataset, 신용공여 잔고 및 기간 수집 Job API")
public class KofiaController {
  private final KofiaCollectionService service;

  public KofiaController(KofiaCollectionService service) {
    this.service = service;
  }

  @GetMapping("/datasets")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA Dataset Registry 조회")
  public List<DatasetView> datasets() {
    return service.datasets();
  }

  @PostMapping("/{datasetCode}")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA Dataset 날짜 또는 기간 직접 수집")
  public CollectionView collect(
      @PathVariable String datasetCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.collect(KofiaDataset.fromCode(datasetCode), from, to, null);
  }

  @GetMapping("/credit-balances")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA 신용공여 잔고 기간 조회")
  public List<Map<String, Object>> creditBalances(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "1000") int limit) {
    return service.creditBalances(from, to, limit);
  }

  @PostMapping("/collection-jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA 기간 수집 비동기 Job 생성")
  public ResponseEntity<JobView> start(@Valid @RequestBody StartJobRequest request) {
    return ResponseEntity.accepted()
        .body(service.startJob(request.from(), request.to(), request.datasetCodes()));
  }

  @GetMapping("/collection-jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA 수집 Job 목록 조회")
  public List<JobView> jobs(@RequestParam(defaultValue = "20") int limit) {
    return service.jobs(limit);
  }

  @GetMapping("/collection-jobs/{jobId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA 수집 Job 상세 및 항목 상태 조회")
  public JobView job(@PathVariable UUID jobId) {
    return service.job(jobId);
  }

  @PostMapping("/collection-jobs/{jobId}/retry-failures")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA 수집 Job 실패 기간 재실행")
  public ResponseEntity<JobView> retry(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(service.retryFailures(jobId));
  }

  public record StartJobRequest(
      @NotNull LocalDate from, @NotNull LocalDate to, List<String> datasetCodes) {}
}
