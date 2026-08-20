package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.*;
import com.nanum.investment.briefing.dto.request.MarketScenarioProbabilityDto;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MarketDirectionPredictionServiceTest {
  @Test void normalizedProbabilitiesAlwaysSumToOneHundred() {
    int[] values = MarketDirectionPredictionService.normalize(31, 40, 22, 7);
    assertThat(Arrays.stream(values).sum()).isEqualTo(100);
    assertThat(Arrays.stream(values).boxed()).allMatch(value -> value >= 0 && value <= 100);
  }
  @Test void rejectsScenarioProbabilityWhenSumIsNotOneHundred() {
    assertThatThrownBy(() -> MarketDirectionPredictionService.validate(
        new MarketScenarioProbabilityDto(25,40,25,11,0,0,0,0)))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("합계가 100");
  }
}
