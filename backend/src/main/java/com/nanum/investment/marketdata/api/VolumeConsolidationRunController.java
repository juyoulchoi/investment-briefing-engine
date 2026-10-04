package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.VolumeConsolidationRunService;
import com.nanum.investment.marketdata.domain.VolumeConsolidation.Screen;
import com.nanum.investment.marketdata.domain.VolumeConsolidationRun;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/krx/screens/volume-consolidation/runs")
@io.swagger.v3.oas.annotations.tags.Tag(name = "KRX 조건검색")
public class VolumeConsolidationRunController {
  private final VolumeConsolidationRunService runs;

  public VolumeConsolidationRunController(VolumeConsolidationRunService runs) {
    this.runs = runs;
  }

  @GetMapping
  public List<VolumeConsolidationRun> latest(@RequestParam(defaultValue = "10") int limit) {
    return runs.latest(limit);
  }

  @GetMapping("/{id}/result")
  public Screen result(@PathVariable UUID id) {
    return runs.result(id);
  }

  @PostMapping
  public VolumeConsolidationRun run(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate baseDate) {
    return runs.runManually(baseDate);
  }
}
