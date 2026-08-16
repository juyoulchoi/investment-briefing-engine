package com.nanum.investment.api;

import com.nanum.investment.domain.PortfolioDecision;
import com.nanum.investment.service.BriefingPromptService;
import com.nanum.investment.service.DecisionHistoryService;
import com.nanum.investment.service.PortfolioDecisionService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/investment")
public class InvestmentDecisionController {
  private final PortfolioDecisionService decisions;
  private final BriefingPromptService prompts;
  private final DecisionHistoryService history;

  public InvestmentDecisionController(
      PortfolioDecisionService decisions,
      BriefingPromptService prompts,
      DecisionHistoryService history) {
    this.decisions = decisions;
    this.prompts = prompts;
    this.history = history;
  }

  @PostMapping("/decision")
  public PortfolioDecision decide(@Valid @RequestBody InvestmentDecisionRequest request) {
    PortfolioDecision result = decisions.decide(request);
    history.save(request, result);
    return result;
  }

  @PostMapping("/briefing-prompt")
  public Map<String, String> prompt(@Valid @RequestBody InvestmentDecisionRequest request) {
    return Map.of("prompt", prompts.buildPrompt(decisions.decide(request)));
  }
}
