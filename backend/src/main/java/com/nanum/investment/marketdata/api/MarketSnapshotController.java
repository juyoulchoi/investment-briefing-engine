package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.MarketSnapshotGenerationService;
import com.nanum.investment.marketdata.domain.MarketSnapshotGenerationResult;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/snapshots")
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 분석", description = "시장 스냅샷 및 심리 분석 API")
public class MarketSnapshotController {
  private final MarketSnapshotGenerationService snapshots;

  public MarketSnapshotController(MarketSnapshotGenerationService snapshots) {
    this.snapshots = snapshots;
  }

  @PostMapping("/generate")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 스냅샷 생성")
  public MarketSnapshotGenerationResult generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return snapshots.generate(baseDate);
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 스냅샷 조회")
  public List<Map<String, Object>> find(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return snapshots.find(baseDate);
  }
}
