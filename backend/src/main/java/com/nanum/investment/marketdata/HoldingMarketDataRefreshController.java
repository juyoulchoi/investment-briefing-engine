package com.nanum.investment.marketdata;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data/holdings")
public class HoldingMarketDataRefreshController {
 private final HoldingMarketDataRefreshService refresh;
 public HoldingMarketDataRefreshController(HoldingMarketDataRefreshService refresh){this.refresh=refresh;}
 @PostMapping("/refresh") public HoldingMarketDataRefreshResult refresh(){return refresh.refresh();}
}
