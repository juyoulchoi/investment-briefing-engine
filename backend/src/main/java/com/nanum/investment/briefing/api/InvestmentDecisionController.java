package com.nanum.investment.briefing.api;

import com.nanum.investment.briefing.application.BriefingPromptService;
import com.nanum.investment.briefing.application.DecisionHistoryService;
import com.nanum.investment.briefing.application.PortfolioDecisionService;
import com.nanum.investment.briefing.domain.PortfolioDecision;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/investment")
@io.swagger.v3.oas.annotations.tags.Tag(name = "투자 의사결정", description = "포트폴리오 투자 의사결정 API")
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
  @io.swagger.v3.oas.annotations.Operation(summary = "포트폴리오 투자 의사결정 계산")
  public PortfolioDecision decide(@Valid @RequestBody InvestmentDecisionRequest request) {
    PortfolioDecision result = decisions.decide(request);
    history.save(request, result);
    return result;
  }

  @PostMapping("/briefing-prompt")
  @io.swagger.v3.oas.annotations.Operation(summary = "투자 브리핑 프롬프트 생성")
  public Map<String, String> prompt(@Valid @RequestBody InvestmentDecisionRequest request) {
    return Map.of("prompt", prompts.buildPrompt(decisions.decide(request)));
  }
}
