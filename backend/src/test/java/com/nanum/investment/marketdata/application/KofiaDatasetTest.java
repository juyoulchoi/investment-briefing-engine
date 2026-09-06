package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import java.time.LocalDate;
import java.util.Arrays;
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

  @Test
  void registersAllDocumentedFreeSisServicesWithoutDuplicateServiceIds() {
    assertThat(KofiaDataset.values()).hasSize(18);
    assertThat(Arrays.stream(KofiaDataset.values()).map(KofiaDataset::serviceId).distinct())
        .hasSize(18);
    assertThat(Arrays.stream(KofiaDataset.values()).filter(KofiaDataset::normalized))
        .containsExactlyInAnyOrder(
            KofiaDataset.CREDIT_BALANCE_TREND,
            KofiaDataset.SECURITIES_LENDING_TREND,
            KofiaDataset.MARKET_FUNDS_TREND);
  }

  @Test
  void buildsDateParametersForEachCollectionMode() {
    LocalDate from = LocalDate.of(2024, 1, 1);
    LocalDate to = LocalDate.of(2024, 3, 31);

    assertThat(KofiaDataset.CREDIT_BALANCE_TREND.requestParameters(from, to))
        .containsEntry("tmpV45", "20240101")
        .containsEntry("tmpV46", "20240331")
        .containsEntry("OBJ_NM", "STATSCU0100000070BO");
    assertThat(KofiaDataset.FUND_FLOW_PERIOD.requestParameters(from, to))
        .containsEntry("tmpV30", "20240101")
        .containsEntry("tmpV31", "20240331");
    assertThat(KofiaDataset.CMA_DAILY_STATUS.requestParameters(to, to))
        .containsEntry("tmpV34", "20240331")
        .doesNotContainKeys("tmpV45", "tmpV46");
    assertThat(KofiaDataset.FINAL_QUOTED_YIELD.requestParameters(to, to))
        .containsEntry("tmpV45", "20240331")
        .containsEntry("tmpV46", "20240331")
        .doesNotContainKey("tmpV34");
  }

  @Test
  void canonicalizesDateRowKeysToExistingIsoFormat() throws Exception {
    var row = new ObjectMapper().readTree("{\"TMPV1\":\"20240102\"}");
    assertThat(KofiaDataset.KOSPI_MARKET.rowKey(row, 1)).isEqualTo("2024-01-02");
  }
}
