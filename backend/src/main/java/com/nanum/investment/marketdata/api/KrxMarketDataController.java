package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.KrxMarketDataService;
import com.nanum.investment.marketdata.domain.KrxDataset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/krx")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "KRX 데이터",
    description = "KRX 원본 데이터 및 Collection Job API")
public class KrxMarketDataController {
  private final KrxMarketDataService krx;

  public KrxMarketDataController(KrxMarketDataService krx) {
    this.krx = krx;
  }

  @GetMapping("/api-methods")
  @io.swagger.v3.oas.annotations.Operation(summary = "지원 KRX 원본 API method 목록 조회")
  public List<String> apiMethods() {
    return Arrays.stream(KrxDataset.values()).map(KrxDataset::apiMethod).toList();
  }

  @PostMapping("/{apiMethod}")
  @io.swagger.v3.oas.annotations.Operation(summary = "KRX API method 날짜 데이터 수집")
  public KrxMarketDataService.CollectionResult collect(
      @PathVariable String apiMethod,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return krx.collect(KrxDataset.fromApiMethod(apiMethod), baseDate);
  }

  @GetMapping("/{apiMethod}")
  @io.swagger.v3.oas.annotations.Operation(summary = "저장된 KRX 원본 데이터 조회")
  public List<Map<String, Object>> find(
      @PathVariable String apiMethod,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
      @RequestParam(defaultValue = "100") int limit) {
    return krx.find(KrxDataset.fromApiMethod(apiMethod), baseDate, limit);
  }
}
