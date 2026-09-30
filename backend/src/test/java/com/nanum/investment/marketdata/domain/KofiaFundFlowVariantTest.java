package com.nanum.investment.marketdata.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KofiaFundFlowVariantTest {
  @Test
  void buildsExactFreeSisRequestParameters() {
    KofiaFundFlowVariant variant =
        new KofiaFundFlowVariant(
            UUID.randomUUID(),
            KofiaFundFlowVariant.Stage.FUND_TYPE,
            "02",
            "*",
            "1",
            "*",
            null,
            "Y",
            "hash");

    assertThat(variant.requestParameters(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 31)))
        .containsEntry("tmpV3", "02")
        .containsEntry("tmpV5", "")
        .containsEntry("tmpV7", "1")
        .containsEntry("tmpV11", "")
        .containsEntry("tmpV19", "Y")
        .containsEntry("tmpV30", "20240102")
        .containsEntry("tmpV31", "20240331")
        .containsEntry("tmpV37", "0")
        .containsEntry("tmpV40", "1")
        .containsEntry("tmpV41", "1")
        .containsEntry("OBJ_NM", "STATFND0100100030BO");
  }

  @Test
  void normalizesBlankScopesToAll() {
    KofiaFundFlowVariant variant =
        new KofiaFundFlowVariant(
            UUID.randomUUID(),
            KofiaFundFlowVariant.Stage.AGGREGATE,
            "",
            null,
            "2",
            " ",
            null,
            "Y",
            "hash");

    assertThat(variant.fundTypeCode()).isEqualTo("*");
    assertThat(variant.fundKindCode()).isEqualTo("*");
    assertThat(variant.managerCode()).isEqualTo("*");
  }
}
