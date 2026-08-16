package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.briefing.domain.PortfolioDecision;
import org.springframework.stereotype.Service;

@Service
public class BriefingPromptService {
  private static final String INSTRUCTIONS =
      """
            아래 JSON은 계산 완료된 투자판단 결과다. 계산값을 바꾸지 말고 한국어 브리핑을 작성하라.
            반드시 포함: 시장국면, 시장심리와 신뢰도, AI 투자 피로, 펀더멘털 훼손 여부, 수급, 환율·금리,
            정기매수 신호, 권장 배수와 금액, 대기현금, 추가매수 우선순위, 심리 반전 신호,
            다음주 실행, 월간 리밸런싱, 위험요인, 종합판단.
            사실과 추론을 구분하라.
            JSON:
            """;

  private final ObjectMapper objectMapper;

  public BriefingPromptService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String buildPrompt(PortfolioDecision decision) {
    try {
      return INSTRUCTIONS
          + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(decision);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("프롬프트 생성 실패", e);
    }
  }
}
