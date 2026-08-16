package com.nanum.investment.briefing.api;

import com.nanum.investment.regularbuy.application.AdditionalBuyViewResult;
import com.nanum.investment.regularbuy.application.AdditionalBuyViewService;
import com.nanum.investment.regularbuy.application.DailyBuyPlanResult;
import com.nanum.investment.regularbuy.application.DailyBuyPlanService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investment/buy-plans")
public class DailyBuyPlanController {
  private final DailyBuyPlanService plans;
  private final AdditionalBuyViewService additionalBuys;

  public DailyBuyPlanController(
      DailyBuyPlanService plans, AdditionalBuyViewService additionalBuys) {
    this.plans = plans;
    this.additionalBuys = additionalBuys;
  }

  @PostMapping("/calculate")
  public DailyBuyPlanResult calculate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return plans.calculateAndSave(baseDate);
  }

  @GetMapping("/additional/latest")
  public AdditionalBuyViewResult latestAdditional() {
    return additionalBuys.latest();
  }

  @GetMapping
  public List<Map<String, Object>> find(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return plans.find(baseDate);
  }
}
