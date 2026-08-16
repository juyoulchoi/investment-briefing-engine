package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.MarketSnapshotGenerationService;
import com.nanum.investment.marketdata.domain.MarketSnapshotGenerationResult;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/snapshots")
public class MarketSnapshotController {
  private final MarketSnapshotGenerationService snapshots;

  public MarketSnapshotController(MarketSnapshotGenerationService snapshots) {
    this.snapshots = snapshots;
  }

  @PostMapping("/generate")
  public MarketSnapshotGenerationResult generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return snapshots.generate(baseDate);
  }

  @GetMapping
  public List<Map<String, Object>> find(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return snapshots.find(baseDate);
  }
}
