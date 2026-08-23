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
@io.swagger.v3.oas.annotations.tags.Tag(name = "매수 계획", description = "일일 및 추가 매수계획 API")
public class DailyBuyPlanController {
  private final DailyBuyPlanService plans;
  private final AdditionalBuyViewService additionalBuys;

  public DailyBuyPlanController(
      DailyBuyPlanService plans, AdditionalBuyViewService additionalBuys) {
    this.plans = plans;
    this.additionalBuys = additionalBuys;
  }

  @PostMapping("/calculate")
  @io.swagger.v3.oas.annotations.Operation(summary = "일일 매수계획 계산")
  public DailyBuyPlanResult calculate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return plans.calculateAndSave(baseDate);
  }

  @GetMapping("/additional/latest")
  @io.swagger.v3.oas.annotations.Operation(summary = "최신 추가매수 결과 조회")
  public AdditionalBuyViewResult latestAdditional() {
    return additionalBuys.latest();
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "매수계획 조회")
  public List<Map<String, Object>> find(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return plans.find(baseDate);
  }
}
