package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyBriefingImportServiceTest {
  private final LegacyBriefingImportService service =
      new LegacyBriefingImportService(null, new ObjectMapper());

  @Test
  void extractsOnlyExplicitRiskScore() throws Exception {
    assertThat(riskCandidates("오늘 시장 위험지수는 **42점 / 100점**입니다.")).containsExactly(42);
    assertThat(riskCandidates("위험이 높지만 점수는 제시하지 않습니다.")).isEmpty();
  }

  @Test
  void keepsMultipleDistinctRiskScoresForReview() throws Exception {
    assertThat(riskCandidates("어제 위험지수 42점, 오늘 시장 위험지수는 47점입니다."))
        .containsExactly(42, 47);
  }

  @SuppressWarnings("unchecked")
  private List<Integer> riskCandidates(String text) throws Exception {
    Method method = LegacyBriefingImportService.class.getDeclaredMethod("riskCandidates", String.class);
    method.setAccessible(true);
    return (List<Integer>) method.invoke(service, text);
  }
}
