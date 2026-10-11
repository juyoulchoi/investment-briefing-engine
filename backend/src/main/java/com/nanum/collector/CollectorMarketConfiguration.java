package com.nanum.collector;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/** Source services only: no investment decisions, portfolios or briefing jobs. */
@Configuration
@ConditionalOnProperty(name = "MARKET_SOURCE_ENABLED", havingValue = "true")
@ComponentScan(basePackages = {"com.nanum.investment.marketdata", "com.nanum.investment.common.application", "com.nanum.investment.holding.application", "com.nanum.investment.briefing.application"},
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
      ".*\\.(KrxMarketDataService|KrxCollectionJobService|KrxCollectionJobRunner|KrxCollectionOutcomeClassifier|KrxBackfillService|KrxBackfillJobRunner|KrxCollectionJobRepository|KrxBackfillRepository|KrxAsyncConfig|KrxRequestRateLimiter|KrxIndexDailyCollector|KrxDerivativeDailyCollector|KrxBondTradingDailyCollector)",
      ".*\\.(YahooIndexService|OverseasStockService|FredBondYieldService|FredBondYieldCollector|FredRestClient|FredRequestRateLimiter|FredMacroService|FredMacroRepository|FredMacroJobRunner|CommonCodeLookupService|HoldingPriceSyncService|MarketCalendarService|CollectionJobReprocessingService)"
    }))
public class CollectorMarketConfiguration {}
