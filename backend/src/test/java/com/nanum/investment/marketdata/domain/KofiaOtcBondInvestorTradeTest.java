package com.nanum.investment.marketdata.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KofiaOtcBondInvestorTradeTest {
  @Test
  void expandsWideFreeSisRowIntoInvestorMetricsWithoutInventingMissingCustomerField()
      throws Exception {
    var row =
        new ObjectMapper()
            .readTree(
                """
                {"TMPV1":"순매수","TMPV2":"국채","TMPV3":6497010,"TMPV4":null,
                 "TMPV5":786753,"TMPV14":47853}
                """);
    Map<String, Object> parameters =
        Map.of(
            "tmpV1", "D",
            "tmpV40", "100000000",
            "tmpV67", "1",
            "tmpV76", "01",
            "tmpV77", "02",
            "tmpV92", "03",
            "tmpV93", "06");

    var values =
        KofiaOtcBondInvestorTrade.from(
            row, LocalDate.of(2026, 6, 23), LocalDate.of(2026, 9, 23), parameters);

    assertThat(values)
        .extracting(KofiaOtcBondInvestorTrade::investorTypeCode)
        .containsExactly("ALL", "INTERDEALER", "BANK", "INDIVIDUAL")
        .doesNotContain("CUSTOMER");
    assertThat(values.get(0).dataPeriodCode()).isEqualTo("D");
    assertThat(values.get(0).valueTypeCode()).isEqualTo("1");
    assertThat(values.get(0).remainingFromMonths()).isEqualTo(15);
    assertThat(values.get(0).remainingToMonths()).isEqualTo(30);
    assertThat(values.get(0).unitMultiplier()).isEqualByComparingTo(new BigDecimal("100000000"));
    assertThat(values.get(0).value()).isEqualByComparingTo(new BigDecimal("6497010"));
    assertThat(values.get(1).value()).isNull();
  }

  @Test
  void mapsCustomerTradingWhenProviderActuallyReturnsG001() throws Exception {
    var row = new ObjectMapper().readTree("{\"TMPV1\":\"총거래\",\"TMPV2\":\"합계\",\"G001\":123}");

    var values =
        KofiaOtcBondInvestorTrade.from(
            row,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            Map.of(
                "tmpV1", "D",
                "tmpV40", "100000000",
                "tmpV67", "1",
                "tmpV76", "00",
                "tmpV77", "00",
                "tmpV92", "00",
                "tmpV93", "00"));

    assertThat(values)
        .singleElement()
        .satisfies(
            value -> {
              assertThat(value.investorTypeCode()).isEqualTo("CUSTOMER");
              assertThat(value.investorTypeName()).isEqualTo("대고객매매");
              assertThat(value.value()).isEqualByComparingTo("123");
            });
  }
}
