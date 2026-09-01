package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nanum.investment.marketdata.domain.KofiaDataset;
import org.junit.jupiter.api.Test;

class KofiaDatasetTest {
  @Test
  void resolvesRegistryCodeServiceIdAndObjectName() {
    assertThat(KofiaDataset.fromCode("credit_balance_trend"))
        .isEqualTo(KofiaDataset.CREDIT_BALANCE_TREND);
    assertThat(KofiaDataset.fromCode("STATSCU0100000070"))
        .isEqualTo(KofiaDataset.CREDIT_BALANCE_TREND);
    assertThat(KofiaDataset.fromCode("STATSCU0100000070BO"))
        .isEqualTo(KofiaDataset.CREDIT_BALANCE_TREND);
    assertThat(KofiaDataset.fromCode("STATSCU0100000140"))
        .isEqualTo(KofiaDataset.SECURITIES_LENDING_TREND);
    assertThat(KofiaDataset.fromCode("STATSCU0100000060BO"))
        .isEqualTo(KofiaDataset.MARKET_FUNDS_TREND);
  }

  @Test
  void rejectsUnknownDataset() {
    assertThatThrownBy(() -> KofiaDataset.fromCode("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("지원하지 않는 KOFIA Dataset");
  }
}
