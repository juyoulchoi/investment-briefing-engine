package com.nanum.investment.marketdata;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/market-data/validation")
public class MarketDataValidationController {
 private final MarketDataConsistencyService validation;
 public MarketDataValidationController(MarketDataConsistencyService validation){this.validation=validation;}
 @PostMapping public MarketDataValidationResult validate(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate baseDate){return validation.validate(baseDate);}
}
