package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.FredBondYieldService;
import com.nanum.investment.marketdata.application.KrxMarketDataService;
import com.nanum.investment.marketdata.infrastructure.OverseasStockService;
import com.nanum.investment.marketdata.infrastructure.YahooIndexService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data")
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 데이터", description = "Yahoo·FRED·환율 및 시장데이터 API")
public class MarketDataController {
  private final KrxMarketDataService krx;
  private final OverseasStockService overseas;
  private final YahooIndexService yahooIndices;
  private final FredBondYieldService fredBonds;

  public MarketDataController(
      KrxMarketDataService krx,
      OverseasStockService overseas,
      YahooIndexService yahooIndices,
      FredBondYieldService fredBonds) {
    this.krx = krx;
    this.overseas = overseas;
    this.yahooIndices = yahooIndices;
    this.fredBonds = fredBonds;
  }

  @GetMapping("/indices")
  @io.swagger.v3.oas.annotations.Operation(summary = "등록 시장지수 목록 조회")
  public List<Map<String, Object>> indices() {
    return yahooIndices.indices();
  }

  @PostMapping("/indices/collect")
  @io.swagger.v3.oas.annotations.Operation(summary = "Yahoo 전체 시장지수 기간 수집")
  public List<YahooIndexService.CollectionResult> collectIndices(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return yahooIndices.collectAll(from, to);
  }

  @PostMapping("/indices/{indexCode}/history/collect")
  @io.swagger.v3.oas.annotations.Operation(summary = "Yahoo 개별 시장지수 기간 수집")
  public YahooIndexService.CollectionResult collectIndexHistory(
      @PathVariable String indexCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return yahooIndices.collect(indexCode, from, to);
  }

  @PostMapping("/indices/{indexCode}/refresh")
  @io.swagger.v3.oas.annotations.Operation(summary = "Yahoo 개별 시장지수 최근 데이터 갱신")
  public YahooIndexService.CollectionResult refreshIndex(@PathVariable String indexCode) {
    return yahooIndices.refresh(indexCode);
  }

  @GetMapping("/indices/{indexCode}/history")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장지수 기간 이력 조회")
  public List<Map<String, Object>> indexHistory(
      @PathVariable String indexCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return yahooIndices.history(indexCode, from, to);
  }

  @GetMapping("/indices/{indexCode}")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장지수 최신값 조회")
  public Map<String, Object> latestIndex(@PathVariable String indexCode) {
    return yahooIndices.latest(indexCode);
  }

  @PostMapping("/bonds/collect")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED 채권금리 기간 수집")
  public FredBondYieldService.CollectionResult collectBonds(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return fredBonds.collect(from, to);
  }

  @PostMapping("/bonds/refresh")
  @io.swagger.v3.oas.annotations.Operation(summary = "FRED 채권금리 최근 데이터 갱신")
  public FredBondYieldService.RefreshResult refreshBonds() {
    return fredBonds.refreshLatest();
  }

  @GetMapping("/bonds")
  @io.swagger.v3.oas.annotations.Operation(summary = "채권금리 기간 데이터 조회")
  public List<Map<String, Object>> bonds(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return fredBonds.history(from, to);
  }

  @GetMapping("/overseas")
  @io.swagger.v3.oas.annotations.Operation(summary = "해외종목 목록 조회")
  public List<Map<String, Object>> overseasStocks() {
    return overseas.findAll();
  }

  @GetMapping("/domestic")
  @io.swagger.v3.oas.annotations.Operation(summary = "국내종목 목록 조회")
  public List<Map<String, Object>> domesticStocks() {
    return krx.findLatestStocks();
  }

  @PostMapping("/overseas/{symbol}/history/collect")
  @io.swagger.v3.oas.annotations.Operation(summary = "Yahoo 해외종목 기간 데이터 수집")
  public OverseasStockService.HistoryCollectionResult collectHistory(
      @PathVariable String symbol,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return overseas.collectHistory(symbol, from, to);
  }

  @GetMapping("/overseas/{symbol}/history")
  @io.swagger.v3.oas.annotations.Operation(summary = "해외종목 기간 데이터 조회")
  public List<Map<String, Object>> overseasHistory(
      @PathVariable String symbol,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return overseas.findHistory(symbol, from, to);
  }

  @PostMapping("/overseas/{symbol}/refresh")
  @io.swagger.v3.oas.annotations.Operation(summary = "Yahoo 해외종목 최근 데이터 갱신")
  public Map<String, Object> refresh(@PathVariable String symbol) {
    return overseas.refresh(symbol);
  }

  @GetMapping("/overseas/{symbol}")
  @io.swagger.v3.oas.annotations.Operation(summary = "해외종목 최신 데이터 조회")
  public Map<String, Object> overseas(@PathVariable String symbol) {
    return overseas.find(symbol);
  }
}
