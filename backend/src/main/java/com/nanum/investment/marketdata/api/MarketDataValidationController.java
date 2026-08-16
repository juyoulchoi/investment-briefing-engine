package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.MarketDataConsistencyService;
import com.nanum.investment.marketdata.domain.MarketDataValidationResult;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/validation")
public class MarketDataValidationController {
  private final MarketDataConsistencyService validation;

  public MarketDataValidationController(MarketDataConsistencyService validation) {
    this.validation = validation;
  }

  @PostMapping
  public MarketDataValidationResult validate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return validation.validate(baseDate);
  }
}
