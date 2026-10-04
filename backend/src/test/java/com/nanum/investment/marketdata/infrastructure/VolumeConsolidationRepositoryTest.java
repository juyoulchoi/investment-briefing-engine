package com.nanum.investment.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VolumeConsolidationRepositoryTest {
  @Test
  void malformedAndMissingAmountsRemainUnknownRatherThanZero() {
    assertThat(VolumeConsolidationRepository.number("1,234.5")).isEqualByComparingTo("1234.5");
    assertThat(VolumeConsolidationRepository.number("-")).isNull();
    assertThat(VolumeConsolidationRepository.number(null)).isNull();
    assertThat(VolumeConsolidationRepository.number(" ")).isNull();
  }
}
