package com.nanum.investment.rebalance.api;

import com.nanum.investment.rebalance.application.AutomaticRebalanceResult;
import com.nanum.investment.rebalance.application.AutomaticRebalanceService;
import com.nanum.investment.rebalance.domain.RebalanceType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investment/rebalancing")
@io.swagger.v3.oas.annotations.tags.Tag(name = "리밸런싱", description = "자동 리밸런싱 API")
public class RebalanceController {
  private final AutomaticRebalanceService rebalancing;

  public RebalanceController(AutomaticRebalanceService rebalancing) {
    this.rebalancing = rebalancing;
  }

  @PostMapping("/generate")
  @io.swagger.v3.oas.annotations.Operation(summary = "자동 리밸런싱 결과 생성")
  public List<AutomaticRebalanceResult> generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return rebalancing.generate(baseDate);
  }

  @GetMapping("/latest")
  @io.swagger.v3.oas.annotations.Operation(summary = "최신 리밸런싱 결과 조회")
  public AutomaticRebalanceResult latest(@RequestParam RebalanceType type) {
    return rebalancing.latest(type);
  }
}
