package com.nanum.investment.marketdata;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/holdings")
public class HoldingMarketDataRefreshController {
  private final BriefingRefreshService refresh;

  public HoldingMarketDataRefreshController(BriefingRefreshService refresh) {
    this.refresh = refresh;
  }

  @PostMapping("/refresh")
  public BriefingRefreshResult refresh() {
    return refresh.refresh();
  }

  @PostMapping("/weekly-decision/refresh")
  public BriefingRefreshResult refreshWeeklyDecision() {
    return refresh.refreshWeeklyDecision();
  }
}
