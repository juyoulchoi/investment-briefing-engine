package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.ExchangeRateService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/exchange-rates")
public class ExchangeRateController {
  private final ExchangeRateService exchangeRates;

  public ExchangeRateController(ExchangeRateService exchangeRates) {
    this.exchangeRates = exchangeRates;
  }

  @PostMapping("/collect")
  public ExchangeRateService.CollectionResult collect(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return exchangeRates.collect(from, to);
  }

  @GetMapping
  public List<Map<String, Object>> history(
      @RequestParam(defaultValue = "USD") String baseCurrency,
      @RequestParam(defaultValue = "KRW") String quoteCurrency,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return exchangeRates.history(baseCurrency, quoteCurrency, from, to);
  }
}
