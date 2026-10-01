package com.nanum.investment.marketdata.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KofiaEquityMarketStatisticTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void mapsKospiDisplayValuesAndTheirUnitMultipliers() throws Exception {
    var row =
        objectMapper.readTree(
            """
            {"TMPV1":"20260923","TMPV2":7080.92,"TMPV3":23700,"TMPV4":226227,
             "TMPV5":58523614,"TMPV6":23468269,"TMPV7":40.101}
            """);

    var value =
        KofiaEquityMarketStatistic.from(
            KofiaDataset.KOSPI_MARKET, row, Map.of("tmpV40", "100000000", "tmpV41", "10000"));

    assertThat(value.marketCode()).isEqualTo("KOSPI");
    assertThat(value.indexValue()).isEqualByComparingTo("7080.92");
    assertThat(value.tradingQuantity()).isEqualByComparingTo("23700");
    assertThat(value.tradingAmount()).isEqualByComparingTo("226227");
    assertThat(value.marketCapitalization()).isEqualByComparingTo("58523614");
    assertThat(value.foreignMarketCapitalization()).isEqualByComparingTo("23468269");
    assertThat(value.foreignMarketCapitalizationRate()).isEqualByComparingTo("40.101");
    assertThat(value.quantityUnitMultiplier()).isEqualByComparingTo("10000");
    assertThat(value.amountUnitMultiplier()).isEqualByComparingTo("100000000");
  }

  @Test
  void distinguishesKosdaqAndRejectsUnrelatedDataset() throws Exception {
    var row =
        objectMapper.readTree(
            "{\"TMPV2\":844.48,\"TMPV3\":64588,\"TMPV4\":80869,\"TMPV5\":4744019,\"TMPV6\":550613,\"TMPV7\":11.606}");

    assertThat(
            KofiaEquityMarketStatistic.from(
                    KofiaDataset.KOSDAQ_MARKET,
                    row,
                    Map.of("tmpV40", "100000000", "tmpV41", "10000"))
                .marketCode())
        .isEqualTo("KOSDAQ");
    assertThatThrownBy(
            () ->
                KofiaEquityMarketStatistic.from(
                    KofiaDataset.CREDIT_BALANCE_TREND, row, Map.of("tmpV40", "1", "tmpV41", "1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("주식시장 통계 Dataset");
  }
}
