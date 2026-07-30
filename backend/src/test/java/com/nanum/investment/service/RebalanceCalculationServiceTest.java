package com.nanum.investment.service;

import com.nanum.investment.domain.RebalanceAction;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class RebalanceCalculationServiceTest {
 private final RebalanceCalculationService service=new RebalanceCalculationService();

 @Test void buyRecommendationUsesSmallestLimit(){
  var result=service.item(bd("1000"),bd("100"),bd("30"),bd("150"),bd("120"),bd("500"),false,false);
  assertThat(result.recommendedBuyAmount()).isEqualByComparingTo("120");
  assertThat(result.recommendedSellAmount()).isZero();
  assertThat(result.action()).isEqualTo(RebalanceAction.BUY);
 }

 @Test void sellRecommendationNeverCreatesBuyAtSameTime(){
  var result=service.item(bd("1000"),bd("500"),bd("20"),bd("500"),bd("500"),bd("180"),false,false);
  assertThat(result.recommendedBuyAmount()).isZero();
  assertThat(result.recommendedSellAmount()).isEqualByComparingTo("180");
  assertThat(result.action()).isEqualTo(RebalanceAction.SELL);
 }

 @Test void tradeLimitBlocksExecutionRecommendation(){
  var result=service.item(bd("1000"),bd("100"),bd("30"),bd("500"),bd("500"),bd("500"),false,true);
  assertThat(result.recommendedBuyAmount()).isZero();
  assertThat(result.recommendedSellAmount()).isZero();
  assertThat(result.action()).isEqualTo(RebalanceAction.HOLD);
 }

 private BigDecimal bd(String value){return new BigDecimal(value);}
}
