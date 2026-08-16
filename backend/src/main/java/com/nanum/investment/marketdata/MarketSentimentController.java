package com.nanum.investment.marketdata;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/sentiments")
public class MarketSentimentController {
  private final MarketSentimentGenerationService sentiments;

  public MarketSentimentController(MarketSentimentGenerationService sentiments) {
    this.sentiments = sentiments;
  }

  @PostMapping("/generate")
  public MarketSentimentGenerationResult generate(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return sentiments.generate(baseDate);
  }
}
