package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.KrxCollectionJobService;
import com.nanum.investment.marketdata.infrastructure.KrxCollectionJobRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/krx")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "KRX 데이터",
    description = "KRX 원본 데이터 및 Collection Job API")
public class KrxCollectionJobController {
  private final KrxCollectionJobService jobs;

  public KrxCollectionJobController(KrxCollectionJobService jobs) {
    this.jobs = jobs;
  }

  @PostMapping("/collection-jobs")
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX 비동기 Collection Job 생성")
  public ResponseEntity<KrxCollectionJobRepository.JobView> start(@RequestParam String baseDate) {
    return ResponseEntity.accepted().body(jobs.start(parseDate(baseDate)));
  }

  @GetMapping("/collection-jobs/{jobId}")
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX Collection Job 상세 조회")
  public KrxCollectionJobRepository.JobView find(@PathVariable UUID jobId) {
    return jobs.find(jobId);
  }

  private LocalDate parseDate(String value) {
    if (value != null && value.matches("\\d{8}"))
      return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
    return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
  }
}
