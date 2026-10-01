package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
            KofiaDataset.MARKET_FUNDS_TREND,
            KofiaDataset.OTC_INVESTOR_TRADING,
            KofiaDataset.FINAL_QUOTED_YIELD,
            KofiaDataset.KOSPI_MARKET,
            KofiaDataset.KOSDAQ_MARKET);
  }

  @Test
  void mapsScheduledAdditionalDatasetsToExpectedFreeSisObjects() {
    Map<KofiaDataset, String> expected =
        Map.ofEntries(
            Map.entry(KofiaDataset.SECURITIES_LENDING_DETAILS, "STATSCU0100000130BO"),
            Map.entry(KofiaDataset.CMA_DAILY_STATUS, "STATSCU0100000090BO"),
            Map.entry(KofiaDataset.CMA_BALANCE_TREND, "STATSCU0100000110BO"),
            Map.entry(KofiaDataset.FUND_FLOW_PERIOD, "STATFND0100100030BO"),
            Map.entry(KofiaDataset.CUSTOMER_TYPE_FUND_SCALE_PERIOD, "STATFND0100200180BO"),
            Map.entry(KofiaDataset.ASSET_MANAGER_FUND_FLOW, "STATFND0200100040BO"),
            Map.entry(KofiaDataset.OTC_INVESTOR_TRADING, "STATBND0100000270BO"),
            Map.entry(KofiaDataset.FINAL_QUOTED_YIELD, "STATBND0100000010BO"),
            Map.entry(KofiaDataset.KOSPI_MARKET, "STATSCU0100000020BO"),
            Map.entry(KofiaDataset.KOSDAQ_MARKET, "STATSCU0100000030BO"));

    expected.forEach(
        (dataset, objectName) -> {
          assertThat(dataset.path()).isEqualTo("/meta/getMetaDataList.do");
          assertThat(dataset.objectName()).isEqualTo(objectName);
        });
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
    assertThat(KofiaDataset.CUSTOMER_TYPE_FUND_SCALE_PERIOD.requestParameters(to, to))
        .containsEntry("tmpV34", "20240331")
        .containsEntry("tmpV38", "1")
        .containsEntry("OBJ_NM", "STATFND0100200180BO")
        .doesNotContainKeys("tmpV30", "tmpV31");
    assertThat(KofiaDataset.ASSET_MANAGER_FUND_FLOW.requestParameters(to, to))
        .containsEntry("tmpV3", "")
        .containsEntry("tmpV5", "")
        .containsEntry("tmpV7", "")
        .containsEntry("tmpV19", "Y")
        .containsEntry("tmpV34", "20240331")
        .containsEntry("OBJ_NM", "STATFND0200100040BO");
    assertThat(KofiaDataset.OTC_INVESTOR_TRADING.requestParameters(from, to))
        .containsEntry("tmpV1", "D")
        .containsEntry("tmpV40", "100000000")
        .containsEntry("tmpV41", "1")
        .containsEntry("tmpV45", "20240101")
        .containsEntry("tmpV46", "20240331")
        .containsEntry("tmpV67", "1")
        .containsEntry("tmpV76", "00")
        .containsEntry("tmpV77", "00")
        .containsEntry("tmpV92", "00")
        .containsEntry("tmpV93", "00")
        .containsEntry("OBJ_NM", "STATBND0100000270BO");
    assertThat(KofiaDataset.FINAL_QUOTED_YIELD.requestParameters(to, to))
        .containsEntry("tmpV1", "D")
        .containsEntry("tmpV45", "20240331")
        .containsEntry("tmpV46", "")
        .containsEntry("tmpV40", "")
        .containsEntry("tmpV41", "")
        .containsEntry("OBJ_NM", "STATBND0100000010BO")
        .doesNotContainKey("tmpV34");
    assertThat(KofiaDataset.KOSPI_MARKET.requestParameters(from, to))
        .containsEntry("tmpV1", "D")
        .containsEntry("tmpV40", "100000000")
        .containsEntry("tmpV41", "10000")
        .containsEntry("tmpV45", "20240101")
        .containsEntry("tmpV46", "20240331")
        .containsEntry("OBJ_NM", "STATSCU0100000020BO");
    assertThat(KofiaDataset.KOSDAQ_MARKET.requestParameters(from, to))
        .containsEntry("tmpV1", "D")
        .containsEntry("tmpV40", "100000000")
        .containsEntry("tmpV41", "10000")
        .containsEntry("tmpV45", "20240101")
        .containsEntry("tmpV46", "20240331")
        .containsEntry("OBJ_NM", "STATSCU0100000030BO");
    assertThat(KofiaDataset.SECURITIES_LENDING_DETAILS.requestParameters(to, to))
        .containsEntry("tmpV1", "D")
        .containsEntry("tmpV45", "20240331")
        .containsEntry("tmpV46", "20240331")
        .containsEntry("tmpV74", "1,0,,1")
        .containsEntry("OBJ_NM", "STATSCU0100000130BO")
        .doesNotContainKey("tmpV34");
  }

  @Test
  void usesOneForCommonFreeSisControlParameters() {
    LocalDate date = LocalDate.of(2026, 9, 29);

    assertThat(
            Arrays.stream(KofiaDataset.values())
                .filter(
                    dataset ->
                        !List.of(
                                KofiaDataset.OTC_INVESTOR_TRADING,
                                KofiaDataset.FINAL_QUOTED_YIELD,
                                KofiaDataset.KOSPI_MARKET,
                                KofiaDataset.KOSDAQ_MARKET)
                            .contains(dataset)))
        .allSatisfy(
            dataset ->
                assertThat(dataset.requestParameters(date, date))
                    .containsEntry("tmpV40", "1")
                    .containsEntry("tmpV41", "1"));
    assertThat(KofiaDataset.OTC_INVESTOR_TRADING.requestParameters(date, date))
        .containsEntry("tmpV40", "100000000")
        .containsEntry("tmpV41", "1");
  }

  @Test
  void canonicalizesDateRowKeysToExistingIsoFormat() throws Exception {
    var row = new ObjectMapper().readTree("{\"TMPV1\":\"20240102\"}");
    assertThat(KofiaDataset.KOSPI_MARKET.rowKey(row, 1)).isEqualTo("2024-01-02");
  }
}
