package com.nanum.investment.api;

import com.nanum.investment.domain.RebalanceType;
import com.nanum.investment.service.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investment/rebalancing")
public class RebalanceController {
  private final AutomaticRebalanceService rebalancing;

  public RebalanceController(AutomaticRebalanceService rebalancing) {
    this.rebalancing = rebalancing;
  }

  @PostMapping("/generate")
  public List<AutomaticRebalanceResult> generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return rebalancing.generate(baseDate);
  }

  @GetMapping("/latest")
  public AutomaticRebalanceResult latest(@RequestParam RebalanceType type) {
    return rebalancing.latest(type);
  }
}
