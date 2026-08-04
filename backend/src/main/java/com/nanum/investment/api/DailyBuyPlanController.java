package com.nanum.investment.api;

import com.nanum.investment.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/investment/buy-plans")
public class DailyBuyPlanController {
 private final DailyBuyPlanService plans;
 public DailyBuyPlanController(DailyBuyPlanService plans){this.plans=plans;}
 @PostMapping("/calculate") public DailyBuyPlanResult calculate(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate baseDate){return plans.calculateAndSave(baseDate);}
 @GetMapping public List<Map<String,Object>> find(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate baseDate){return plans.find(baseDate);}
}
