package com.nanum.investment.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class KofiaCompanyFundFlowRepositoryTest {
  @Test
  void generatesWeekdaysAndExcludesWeekends() {
    assertThat(
            KofiaCompanyFundFlowRepository.businessWeekdays(
                LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 22)))
        .containsExactly(
            LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22));
  }
}
