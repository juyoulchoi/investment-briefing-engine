package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.KofiaCatalogService;
import com.nanum.investment.marketdata.application.KofiaCollectionService;
import com.nanum.investment.marketdata.application.KofiaCollectionService.CollectionView;
import com.nanum.investment.marketdata.application.KofiaCollectionService.DatasetView;
import com.nanum.investment.marketdata.application.KofiaFundFlowService;
import com.nanum.investment.marketdata.application.KofiaLookupService;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import com.nanum.investment.marketdata.domain.KofiaFundFlowVariant;
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
  private final KofiaCatalogService catalogService;
  private final KofiaLookupService lookupService;
  private final KofiaFundFlowService fundFlowService;

  public KofiaController(
      KofiaCollectionService service,
      KofiaCatalogService catalogService,
      KofiaLookupService lookupService,
      KofiaFundFlowService fundFlowService) {
    this.service = service;
    this.catalogService = catalogService;
    this.lookupService = lookupService;
    this.fundFlowService = fundFlowService;
  }

  @PostMapping("/catalog/sync")
  @io.swagger.v3.oas.annotations.Operation(summary = "FreeSIS 즐겨찾는 통계 및 메타데이터 동기화")
  public KofiaCatalogService.SyncView syncCatalog() {
    return catalogService.sync();
  }

  @GetMapping("/catalog/services")
  @io.swagger.v3.oas.annotations.Operation(summary = "FreeSIS 서비스 카탈로그 조회")
  public List<Map<String, Object>> catalogServices() {
    return catalogService.services();
  }

  @PostMapping("/lookups/collect")
  @io.swagger.v3.oas.annotations.Operation(summary = "FreeSIS 업종 및 회사 검색 기준정보 수집")
  public KofiaLookupService.CollectionView collectLookups() {
    return lookupService.collectAll();
  }

  @GetMapping("/lookups/businesses")
  @io.swagger.v3.oas.annotations.Operation(summary = "FreeSIS 업종 검색 기준정보 조회")
  public List<Map<String, Object>> businesses(@RequestParam String marketType) {
    return lookupService.businesses(marketType);
  }

  @GetMapping("/lookups/companies")
  @io.swagger.v3.oas.annotations.Operation(summary = "FreeSIS 운용회사 및 판매회사 기준정보 조회")
  public List<Map<String, Object>> companies(
      @RequestParam String tableName, @RequestParam String companyType) {
    return lookupService.companies(tableName, companyType);
  }

  @PostMapping("/fund-flows/variants/sync")
  @io.swagger.v3.oas.annotations.Operation(summary = "기간자금유출입 단계별 수집 조합 동기화")
  public KofiaFundFlowService.VariantSyncView syncFundFlowVariants() {
    return fundFlowService.syncVariants();
  }

  @PostMapping("/fund-flows/variants")
  @io.swagger.v3.oas.annotations.Operation(summary = "검증된 기간자금유출입 교차 조합 등록")
  public KofiaFundFlowVariant registerFundFlowVariant(
      @Valid @RequestBody RegisterFundFlowVariantRequest request) {
    return fundFlowService.registerValidVariant(
        request.fundTypeCode(),
        request.fundKindCode(),
        request.offeringTypeCode(),
        request.managerCode(),
        request.etfIncludeYn());
  }

  @GetMapping("/fund-flows/variants")
  @io.swagger.v3.oas.annotations.Operation(summary = "기간자금유출입 수집 조합 조회")
  public List<Map<String, Object>> fundFlowVariants(
      @RequestParam(defaultValue = "ALL_ACTIVE") String stage) {
    return fundFlowService.variants(KofiaFundFlowVariant.Stage.from(stage));
  }

  @PostMapping("/fund-flows/jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "기간자금유출입 단계별 비동기 수집 Job 생성")
  public ResponseEntity<
          com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.JobView>
      startFundFlowJob(@Valid @RequestBody StartFundFlowJobRequest request) {
    return ResponseEntity.accepted()
        .body(
            fundFlowService.startJob(
                KofiaFundFlowVariant.Stage.from(request.stage()), request.from(), request.to()));
  }

  @GetMapping("/fund-flows/jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "기간자금유출입 수집 Job 목록 조회")
  public List<com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.JobView>
      fundFlowJobs(@RequestParam(defaultValue = "20") int limit) {
    return fundFlowService.jobs(limit);
  }

  @GetMapping("/fund-flows/jobs/{jobId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "기간자금유출입 수집 Job 상세 조회")
  public com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.JobView fundFlowJob(
      @PathVariable UUID jobId, @RequestParam(defaultValue = "false") boolean includeItems) {
    return fundFlowService.job(jobId, includeItems);
  }

  @PostMapping("/fund-flows/jobs/{jobId}/retry-failures")
  @io.swagger.v3.oas.annotations.Operation(summary = "기간자금유출입 수집 Job 실패 항목 재실행")
  public ResponseEntity<
          com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.JobView>
      retryFundFlowJob(@PathVariable UUID jobId) {
    return ResponseEntity.accepted().body(fundFlowService.retryFailures(jobId));
  }

  @GetMapping("/fund-flows/rows")
  @io.swagger.v3.oas.annotations.Operation(summary = "조건 조합별 기간자금유출입 일별 데이터 조회")
  public List<Map<String, Object>> fundFlowRows(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "ALL_ACTIVE") String stage,
      @RequestParam(defaultValue = "1000") int limit) {
    return fundFlowService.rows(from, to, KofiaFundFlowVariant.Stage.from(stage), limit);
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

  @GetMapping("/{datasetCode}/rows")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOFIA Dataset 공통 원천행 기간 조회")
  public List<Map<String, Object>> dataRows(
      @PathVariable String datasetCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "1000") int limit) {
    return service.dataRows(KofiaDataset.fromCode(datasetCode), from, to, limit);
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

  public record StartFundFlowJobRequest(
      @NotNull LocalDate from, @NotNull LocalDate to, @NotNull String stage) {}

  public record RegisterFundFlowVariantRequest(
      @NotNull String fundTypeCode,
      @NotNull String fundKindCode,
      @NotNull String offeringTypeCode,
      @NotNull String managerCode,
      @NotNull String etfIncludeYn) {}
}
