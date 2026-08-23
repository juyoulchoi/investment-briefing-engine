package com.nanum.investment.marketdata.api;

import com.nanum.investment.marketdata.application.BriefingRefreshService;
import com.nanum.investment.marketdata.domain.BriefingRefreshResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/holdings")
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 데이터", description = "Yahoo·FRED·환율 및 시장데이터 API")
public class HoldingMarketDataRefreshController {
  private final BriefingRefreshService refresh;

  public HoldingMarketDataRefreshController(BriefingRefreshService refresh) {
    this.refresh = refresh;
  }

  @PostMapping("/refresh")
  @io.swagger.v3.oas.annotations.Operation(summary = "보유종목 시장데이터 갱신")
  public BriefingRefreshResult refresh() {
    return refresh.refresh();
  }

  @PostMapping("/weekly-decision/refresh")
  @io.swagger.v3.oas.annotations.Operation(summary = "주간 의사결정용 보유종목 시장데이터 갱신")
  public BriefingRefreshResult refreshWeeklyDecision() {
    return refresh.refreshWeeklyDecision();
  }
}
