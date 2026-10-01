package com.nanum.investment.marketdata.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KofiaCustomerScaleVariantTest {
  @Test
  void buildsExactFreeSisRequestParameters() {
    KofiaCustomerScaleVariant variant =
        new KofiaCustomerScaleVariant(
            UUID.randomUUID(),
            KofiaCustomerScaleVariant.Stage.REGION,
            "1",
            "*",
            "2",
            "*",
            null,
            "2",
            "hash");

    assertThat(variant.requestParameters(LocalDate.of(2026, 7, 31)))
        .containsEntry("tmpV4", "1")
        .containsEntry("tmpV5", "")
        .containsEntry("tmpV7", "2")
        .containsEntry("tmpV13", "")
        .containsEntry("tmpV34", "20260731")
        .containsEntry("tmpV38", "2")
        .containsEntry("tmpV40", "1")
        .containsEntry("tmpV41", "1")
        .containsEntry("OBJ_NM", "STATFND0100200180BO");
  }

  @Test
  void normalizesBlankScopesToAll() {
    KofiaCustomerScaleVariant variant =
        new KofiaCustomerScaleVariant(
            UUID.randomUUID(),
            KofiaCustomerScaleVariant.Stage.AGGREGATE,
            "",
            null,
            "1",
            " ",
            null,
            "1",
            "hash");

    assertThat(variant.regionCode()).isEqualTo("*");
    assertThat(variant.fundKindCode()).isEqualTo("*");
    assertThat(variant.sellerCode()).isEqualTo("*");
  }
}
