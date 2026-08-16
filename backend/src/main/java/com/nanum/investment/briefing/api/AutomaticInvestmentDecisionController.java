package com.nanum.investment.briefing.api;

import com.nanum.investment.briefing.application.AutomaticInvestmentDecisionResult;
import com.nanum.investment.briefing.application.AutomaticInvestmentDecisionService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investment/decision")
public class AutomaticInvestmentDecisionController {
  private final AutomaticInvestmentDecisionService decisions;

  public AutomaticInvestmentDecisionController(AutomaticInvestmentDecisionService decisions) {
    this.decisions = decisions;
  }

  @PostMapping("/generate")
  public AutomaticInvestmentDecisionResult generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return decisions.generate(baseDate);
  }
}
