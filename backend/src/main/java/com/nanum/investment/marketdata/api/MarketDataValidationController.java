package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.MarketDataConsistencyService;
import com.nanum.investment.marketdata.domain.MarketDataValidationResult;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/validation")
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 데이터", description = "Yahoo·FRED·환율 및 시장데이터 API")
public class MarketDataValidationController {
  private final MarketDataConsistencyService validation;

  public MarketDataValidationController(MarketDataConsistencyService validation) {
    this.validation = validation;
  }

  @PostMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "시장데이터 정합성 검증")
  public MarketDataValidationResult validate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return validation.validate(baseDate);
  }
}
