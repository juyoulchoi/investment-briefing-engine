package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BriefingDisplayTextServiceTest {
  private final BriefingDisplayTextService service = new BriefingDisplayTextService();

  @Test
  void localizesPlainAndMarkdownEscapedCodesForDisplay() {
    Map<String, String> labels =
        Map.of(
            "MEDIUM", "보통",
            "KEEP_REGULAR_BUY", "정기매수 유지",
            "KEEP_MINIMUM", "최소 정기매수 유지");

    String result =
        service.localize("위험등급 MEDIUM, KEEP_REGULAR_BUY와 KEEP\\_MINIMUM을 구분한다.", labels);

    assertThat(result).isEqualTo("위험등급 보통, 정기매수 유지와 최소 정기매수 유지를 구분한다.");
  }

  @Test
  void doesNotReplaceCodeInsideAnotherToken() {
    String result = service.localize("NORMAL NORMALIZED", Map.of("NORMAL", "정상"));

    assertThat(result).isEqualTo("정상 NORMALIZED");
  }

  @Test
  void preservesCopulaEndingsAndAdjustsDirectionalParticle() {
    String result =
        service.localize(
            "KEEP_REGULAR_BUY이며 NORMAL로 전환되고 KEEP_MINIMUM으로 바뀌었다.",
            Map.of(
                "KEEP_REGULAR_BUY", "정기매수 유지",
                "KEEP_MINIMUM", "최소 정기매수 유지",
                "NORMAL", "정상"));

    assertThat(result).isEqualTo("정기매수 유지이며 정상으로 전환되고 최소 정기매수 유지로 바뀌었다.");
  }

  @Test
  void preservesNullAndBlankText() {
    assertThat(service.localize(null, Map.of("WAIT", "관망"))).isNull();
    assertThat(service.localize("", Map.of("WAIT", "관망"))).isEmpty();
  }
}
