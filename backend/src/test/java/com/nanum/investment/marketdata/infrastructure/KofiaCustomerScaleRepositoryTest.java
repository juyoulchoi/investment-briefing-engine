package com.nanum.investment.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class KofiaCustomerScaleRepositoryTest {
  @Test
  void usesLastWeekdayForMonthlySnapshotDates() {
    assertThat(
            KofiaCustomerScaleRepository.monthlySnapshotDates(
                LocalDate.of(2024, 1, 2), LocalDate.of(2024, 6, 30)))
        .containsExactly(
            LocalDate.of(2024, 1, 31),
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2024, 3, 29),
            LocalDate.of(2024, 4, 30),
            LocalDate.of(2024, 5, 31),
            LocalDate.of(2024, 6, 28));
  }
}
