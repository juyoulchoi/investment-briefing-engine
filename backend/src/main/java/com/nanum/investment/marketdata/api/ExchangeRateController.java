package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.ExchangeRateService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/exchange-rates")
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 데이터", description = "Yahoo·FRED·환율 및 시장데이터 API")
public class ExchangeRateController {
  private final ExchangeRateService exchangeRates;

  public ExchangeRateController(ExchangeRateService exchangeRates) {
    this.exchangeRates = exchangeRates;
  }

  @PostMapping("/collect")
  @io.swagger.v3.oas.annotations.Operation(summary = "Yahoo 환율 기간 데이터 수집")
  public ExchangeRateService.CollectionResult collect(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return exchangeRates.collect(from, to);
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "환율 기간 데이터 조회")
  public List<Map<String, Object>> history(
      @RequestParam(defaultValue = "USD") String baseCurrency,
      @RequestParam(defaultValue = "KRW") String quoteCurrency,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return exchangeRates.history(baseCurrency, quoteCurrency, from, to);
  }
}
