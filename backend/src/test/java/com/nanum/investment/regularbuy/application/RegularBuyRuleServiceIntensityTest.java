package com.nanum.investment.regularbuy.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.briefing.domain.StockDecision;
import com.nanum.investment.holding.domain.StockPosition;
import com.nanum.investment.marketdata.domain.MarketAssessment;
import com.nanum.investment.marketdata.domain.MarketRegime;
import com.nanum.investment.marketdata.domain.SentimentAssessment;
import com.nanum.investment.marketdata.domain.SentimentPhase;
import org.junit.jupiter.api.Test;

class RegularBuyRuleServiceIntensityTest {
  private final RegularBuyRuleService service = new RegularBuyRuleService();

  @Test
  void capsWeeklyMultiplierWithUserSetting() {
    StockPosition position =
        new StockPosition(
            "OVERSEAS", "QQQ", "QQQ", 10_000, 0, 1.5, -20, -20, .10, .05, 90, 90, 10, true);
    MarketAssessment market = assessment(MarketRegime.STRONG_CORRECTION);

    StockDecision decision = service.decide(position, market);

    assertThat(decision.multiplier()).isEqualTo(1.5);
    assertThat(decision.recommendedBuyAmount()).isEqualTo(15_000);
    assertThat(decision.maximumBuyAmount()).isEqualTo(15_000);
    assertThat(decision.adjustmentReason()).contains("시장 국면 기본 배수");
    assertThat(decision.cashPlan()).contains("추가 투입분 5000원");
  }

  @Test
  void explainsCashReservedWhenPurchaseIsPaused() {
    StockPosition position =
        new StockPosition(
            "OVERSEAS", "QQQ", "QQQ", 10_000, 30_000, 3, 0, 0, .10, .13, 80, 80, 10, true);

    StockDecision decision = service.decide(position, assessment(MarketRegime.NORMAL));

    assertThat(decision.multiplier()).isZero();
    assertThat(decision.reservedCash()).isEqualTo(10_000);
    assertThat(decision.cashPlan()).contains("10000원을 현금으로 확보");
  }

  private MarketAssessment assessment(MarketRegime regime) {
    return new MarketAssessment(
        regime,
        50,
        new SentimentAssessment(
            SentimentPhase.NEUTRAL, 20, 80, false, java.util.List.of(), java.util.List.of()),
        java.util.List.of());
  }
}
