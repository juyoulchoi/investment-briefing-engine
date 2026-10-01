package com.nanum.investment.marketdata.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KofiaCompanyFundFlowVariantTest {
  @Test
  void buildsExactFreeSisRequestParameters() {
    KofiaCompanyFundFlowVariant variant =
        new KofiaCompanyFundFlowVariant(
            UUID.randomUUID(),
            KofiaCompanyFundFlowVariant.Stage.FUND_TYPE,
            "02",
            "*",
            "*",
            "Y",
            "hash");

    assertThat(variant.requestParameters(LocalDate.of(2026, 9, 22)))
        .containsEntry("tmpV3", "02")
        .containsEntry("tmpV5", "")
        .containsEntry("tmpV7", "")
        .containsEntry("tmpV19", "Y")
        .containsEntry("tmpV34", "20260922")
        .containsEntry("tmpV40", "1")
        .containsEntry("tmpV41", "1")
        .containsEntry("OBJ_NM", "STATFND0200100040BO");
  }
}
