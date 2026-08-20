package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class BriefingNumberFormatterTest {
  private final BriefingNumberFormatter formatter = new BriefingNumberFormatter();
  @Test void formatsDecisionNumbersWithoutDecimalNoise() {
    assertThat(formatter.score(63.0)).isEqualTo("63점");
    assertThat(formatter.percent(35.00)).isEqualTo("35%");
    assertThat(formatter.percentagePoint(5.0)).isEqualTo("+5%p");
  }
  @Test void formatsMarketValuesWithMeaningfulPrecisionAndUnits() {
    assertThat(formatter.decimal(123.40)).isEqualTo("123.4");
    assertThat(formatter.exchangeRate(1348.2)).isEqualTo("1,348원");
    assertThat(formatter.wonFlow(-8420)).isEqualTo("-8,420억원");
    assertThat(formatter.wonFlow(210000)).isEqualTo("21조원");
  }
}
