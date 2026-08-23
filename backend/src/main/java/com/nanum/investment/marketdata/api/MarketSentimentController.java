package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.MarketSentimentGenerationService;
import com.nanum.investment.marketdata.domain.MarketSentimentGenerationResult;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/sentiments")
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 분석", description = "시장 스냅샷 및 심리 분석 API")
public class MarketSentimentController {
  private final MarketSentimentGenerationService sentiments;

  public MarketSentimentController(MarketSentimentGenerationService sentiments) {
    this.sentiments = sentiments;
  }

  @PostMapping("/generate")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 심리 결과 생성")
  public MarketSentimentGenerationResult generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return sentiments.generate(baseDate);
  }
}
