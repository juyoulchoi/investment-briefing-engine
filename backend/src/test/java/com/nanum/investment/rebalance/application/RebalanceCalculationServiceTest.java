package com.nanum.investment.rebalance.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.rebalance.domain.RebalanceAction;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RebalanceCalculationServiceTest {
  private final RebalanceCalculationService service = new RebalanceCalculationService();

  @Test
  void reservedCashIsPartOfCashAndNotAddedToTotalAssetsTwice() {
    var result = service.account(bd("100"), bd("30"), bd("900"), bd("20"), BigDecimal.ZERO);

    assertThat(result.totalAssetAmount()).isEqualByComparingTo("1000");
    assertThat(result.currentCashAmount()).isEqualByComparingTo("100");
    assertThat(result.targetCashAmount()).isEqualByComparingTo("200");
    assertThat(result.buyBudgetAmount()).isZero();
  }

  @Test
  void ordinaryBuyBudgetDoesNotConsumeReservedCash() {
    var result = service.account(bd("300"), bd("250"), bd("700"), bd("20"), BigDecimal.ZERO);

    assertThat(result.cashGapAmount()).isEqualByComparingTo("100");
    assertThat(result.buyBudgetAmount()).isEqualByComparingTo("50");
  }

  @Test
  void buyRecommendationUsesSmallestLimit() {
    var result =
        service.item(
            bd("1000"), bd("100"), bd("30"), bd("150"), bd("120"), bd("500"), false, false);
    assertThat(result.recommendedBuyAmount()).isEqualByComparingTo("120");
    assertThat(result.recommendedSellAmount()).isZero();
    assertThat(result.action()).isEqualTo(RebalanceAction.BUY);
  }

  @Test
  void sellRecommendationNeverCreatesBuyAtSameTime() {
    var result =
        service.item(
            bd("1000"), bd("500"), bd("20"), bd("500"), bd("500"), bd("180"), false, false);
    assertThat(result.recommendedBuyAmount()).isZero();
    assertThat(result.recommendedSellAmount()).isEqualByComparingTo("180");
    assertThat(result.action()).isEqualTo(RebalanceAction.SELL);
  }

  @Test
  void tradeLimitBlocksExecutionRecommendation() {
    var result =
        service.item(bd("1000"), bd("100"), bd("30"), bd("500"), bd("500"), bd("500"), false, true);
    assertThat(result.recommendedBuyAmount()).isZero();
    assertThat(result.recommendedSellAmount()).isZero();
    assertThat(result.action()).isEqualTo(RebalanceAction.HOLD);
  }

  private BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
