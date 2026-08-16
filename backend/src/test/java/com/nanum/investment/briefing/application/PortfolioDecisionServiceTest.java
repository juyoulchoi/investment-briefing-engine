package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.briefing.api.InvestmentDecisionRequest;
import com.nanum.investment.holding.domain.StockPosition;
import com.nanum.investment.marketdata.domain.MarketSnapshot;
import com.nanum.investment.regularbuy.application.RegularBuyRuleService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortfolioDecisionServiceTest {
  @Test
  void correctionIncreasesHealthyAsset() {
    var service =
        new PortfolioDecisionService(
            new MarketRegimeService(new MarketSentimentService()), new RegularBuyRuleService());
    var market =
        new MarketSnapshot(
            -18, 29, -12000, 3000, 9000, -5000, 1.2, 12, 28, 75, 80, 78, 65, "AI 투자 피로");
    var qqq =
        new StockPosition("종합", "QQQ", "QQQ", 4000, 12000, -12, -18, .08, .05, 85, 78, 45, true);
    var result =
        service.decide(
            new InvestmentDecisionRequest(LocalDate.of(2026, 8, 1), market, List.of(qqq), 100000));
    assertThat(result.stockDecisions().getFirst().recommendedBuyAmount()).isGreaterThan(4000);
    assertThat(result.availableAdditionalBuyCash()).isGreaterThanOrEqualTo(100000);
  }
}
