package com.nanum.investment.marketdata.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class KofiaFinalQuotedYieldTest {
  @Test
  void mapsActualFreeSisYieldFieldsAndKeepsNullMorningValue() throws Exception {
    var row =
        new ObjectMapper()
            .readTree(
                """
                {"TMPV1":"CD수익률(91일)","TMPV2":"80일 ~ 100일","TMPV3":null,
                 "TMPV4":"3.22","TMPV5":"0.01","TMPV6":"3.21",
                 "TMPV8":"3.22","TMPV9":"2.68"}
                """);

    var value = KofiaFinalQuotedYield.from(LocalDate.of(2026, 9, 28), row);

    assertThat(value.baseDate()).isEqualTo(LocalDate.of(2026, 9, 28));
    assertThat(value.instrumentName()).isEqualTo("CD수익률(91일)");
    assertThat(value.remainingTermName()).isEqualTo("80일 ~ 100일");
    assertThat(value.morningYieldRate()).isNull();
    assertThat(value.afternoonYieldRate()).isEqualByComparingTo("3.22");
    assertThat(value.dayChangePoint()).isEqualByComparingTo("0.01");
    assertThat(value.previousYieldRate()).isEqualByComparingTo("3.21");
    assertThat(value.yearHighYieldRate()).isEqualByComparingTo("3.22");
    assertThat(value.yearLowYieldRate()).isEqualByComparingTo("2.68");
  }
}
