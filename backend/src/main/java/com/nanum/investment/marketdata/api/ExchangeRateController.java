package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.ExchangeRateService;
import com.nanum.investment.marketdata.infrastructure.ExchangeRateSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/exchange-rates")
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 데이터", description = "Yahoo·FRED·환율 및 시장데이터 API")
public class ExchangeRateController {
  private final ExchangeRateService exchangeRates;
  private final ExchangeRateSource source;

  public ExchangeRateController(ExchangeRateService exchangeRates, ExchangeRateSource source) {
    this.exchangeRates = exchangeRates;
    this.source = source;
  }

  @PostMapping("/collect")
  @io.swagger.v3.oas.annotations.Operation(summary = "Yahoo 환율 기간 데이터 수집")
  public ExchangeRateService.CollectionResult collect(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    if (source.shared()) throw new ResponseStatusException(HttpStatus.CONFLICT, "환율 수집은 공통 수집기에서 실행하세요.");
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

  // Keep the collection-moved status even when the application has a catch-all advice.
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> collectionMoved(ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode()).body(Map.of(
        "code", "EXCHANGE_COLLECTION_MOVED", "message", error.getReason()));
  }
}
