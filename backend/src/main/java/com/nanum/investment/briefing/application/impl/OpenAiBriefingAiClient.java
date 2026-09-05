package com.nanum.investment.briefing.application.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nanum.investment.briefing.api.response.InvestmentBriefingResponse;
import com.nanum.investment.briefing.application.BriefingAiClient;
import com.nanum.investment.briefing.application.BriefingSnapshotService;
import com.nanum.investment.briefing.domain.BriefingType;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiBriefingAiClient implements BriefingAiClient {
  private static final String INSTRUCTIONS =
      """
            DB에서 전달된 숫자, 점수, 확률, 등급, 신호, 계좌별 행동판단은 확정값이다.
            이 값을 재계산하거나 변경하지 않는다.
            OpenAI의 역할은 DB 값을 근거로 사용자가 이해하기 쉬운 설명을 생성하는 것이다.
            DB 값과 다른 숫자나 신호를 생성하지 않는다.
            데이터가 없는 경우 임의 추정하지 않고 '데이터 부족'이라고 표시한다.
            사실과 해석을 구분하고 매수·매도 권유가 아닌 계산 결과 설명으로 작성하라.
            confirmedValues는 입력 JSON의 confirmedValues 객체를 한 글자도 의미 변경 없이 그대로 복사하라.
            title과 items의 title, summary, content는 자연스러운 한국어로 작성하라.
            내부 영문 코드, 데이터셋 코드, 신호 코드를 사용자 설명 문장에 그대로 노출하지 말고 한국어 의미로 풀어 써라.
            다만 종목 티커, 지수의 공식 명칭, 통화 코드처럼 번역하면 식별이 어려운 고유명사는 필요한 경우에만 영문을 병기할 수 있다.
            briefingType, confirmedValues, itemCode, signalCode는 시스템 계약값이므로 번역하거나 변경하지 않는다.
            items에는 다음 15개 코드를 정확히 한 번씩 순서대로 작성하라:
            MARKET_RISK, MARKET_PHASE, US_MARKET, KR_MARKET_PREVIOUS, KR_MARKET_OUTLOOK,
            MARKET_DIRECTION, SECTOR_SIGNALS, REGULAR_BUY, ACCOUNT_ACTIONS, HOLDING_SIGNALS,
            ADDITIONAL_BUYS, REBUY_SIGNALS, ACTION_SIGNAL, SCHEDULE_AND_RISKS, CONCLUSION.
            각 title은 순서대로 '시장 위험지수', '시장 국면', '전일 미국시장', '전날 한국 주식시장 상황',
            '한국시장 예상', '1~4주 시장 방향 예측', '업종별 신호등', '정기매수 판단', '계좌별 행동',
            '보유 종목별 신호등', '추가매수 후보', '재매수 신호', '당일 행동신호', '주요 일정과 위험요인',
            '오늘의 결론'을 정확히 사용한다.
            일일과 주간은 위 제목·번호·순서를 동일하게 사용한다. 주간은 내용만 다음 주 관점으로 설명한다.
            signalCode는 NORMAL, WATCH, CAUTION, RISK 중 하나를 사용하라.
            숫자는 다음 공통 표시 규칙을 지켜라. 점수·확률·확률변화·현금비중·투입비율·일수·건수·종목 수는 정수로 쓰고
            불필요한 .0과 .00을 쓰지 않는다. 실제 시장 소수는 최대 2자리, 원/달러 환율은 원 단위 정수,
            원화 수급과 거래대금은 억원 또는 조원으로 읽기 쉽게 쓴다.
            """;
  private static final String SCHEMA_JSON =
      """
            {
              "type":"object",
              "additionalProperties":false,
              "required":["briefingDate","briefingType","title","confirmedValues","items"],
              "properties":{
                "briefingDate":{"type":"string"},
                "briefingType":{"type":"string","enum":["DAILY","WEEKLY"]},
                "title":{"type":"string"},
                "confirmedValues":{
                  "type":"object","additionalProperties":false,
                  "required":["briefingDate","briefingType","marketRiskScore","marketRiskGrade","marketPhase",
                    "marketDirectionPredictionId","marketDirectionScore","uptrendResume","boxRange","reCorrection","retestLow",
                    "uptrendResumeChange","boxRangeChange","reCorrectionChange","retestLowChange","regularBuySignal",
                    "dailyActionSignal","recommendedCashRatio"],
                  "properties":{
                    "briefingDate":{"type":"string"},"briefingType":{"type":"string"},
                    "marketRiskScore":{"type":"integer"},"marketRiskGrade":{"type":"string"},"marketPhase":{"type":"string"},
                    "marketDirectionPredictionId":{"type":"integer"},"marketDirectionScore":{"type":"integer"},
                    "uptrendResume":{"type":"integer"},"boxRange":{"type":"integer"},"reCorrection":{"type":"integer"},"retestLow":{"type":"integer"},
                    "uptrendResumeChange":{"type":"integer"},"boxRangeChange":{"type":"integer"},"reCorrectionChange":{"type":"integer"},"retestLowChange":{"type":"integer"},
                    "regularBuySignal":{"type":"string"},"dailyActionSignal":{"type":"string"},"recommendedCashRatio":{"type":"integer"}
                  }
                },
                "items":{"type":"array","minItems":15,"maxItems":15,"items":{"$ref":"#/$defs/item"}}
              },
              "$defs":{
                "item":{
                  "type":"object",
                  "additionalProperties":false,
                  "required":["itemCode","title","summary","content","signalCode","actionRequired"],
                  "properties":{
                    "itemCode":{"type":"string"},
                    "title":{"type":"string"},
                    "summary":{"type":"string"},
                    "content":{"type":"string"},
                    "signalCode":{"type":"string","enum":["NORMAL","WATCH","CAUTION","RISK"]},
                    "actionRequired":{"type":"boolean"}
                  }
                }
              }
            }
            """;

  private final JdbcClient jdbc;
  private final BriefingSnapshotService snapshots;
  private final ObjectMapper json;
  private final HttpClient http;
  private final String apiKey;
  private final String model;
  private final URI responsesUri;

  @Autowired
  public OpenAiBriefingAiClient(
      JdbcClient jdbc,
      ObjectMapper json,
      BriefingSnapshotService snapshots,
      @Value("${OPENAI_API_KEY:}") String apiKey,
      @Value("${OPENAI_MODEL:gpt-5.6-sol}") String model,
      @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl) {
    this(
        jdbc,
        json,
        snapshots,
        apiKey,
        model,
        baseUrl,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  OpenAiBriefingAiClient(
      JdbcClient jdbc,
      ObjectMapper json,
      BriefingSnapshotService snapshots,
      String apiKey,
      String model,
      String baseUrl,
      HttpClient http) {
    this.jdbc = jdbc;
    this.json = json;
    this.snapshots = snapshots;
    this.apiKey = apiKey;
    this.model = model;
    this.responsesUri = URI.create(baseUrl.replaceAll("/+$", "") + "/responses");
    this.http = http;
  }

  @Override
  public InvestmentBriefingResponse generateBriefing(BriefingType briefingType) {
    if (!StringUtils.hasText(apiKey))
      throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
    if (briefingType == null) throw new IllegalArgumentException("브리핑 유형이 필요합니다.");
    BriefingSnapshotService.FixedBriefingSnapshot raw = snapshots.latest(briefingType);
    try {
      ObjectNode request = json.createObjectNode();
      String typeInstructions =
          briefingType == BriefingType.WEEKLY
              ? " 지난 월요일부터 금요일까지의 변화, 반복 신호, 주간 요약을 중심으로 설명하라."
              : " 해당 기준일의 일일 상황을 설명하라.";
      request.put("model", model);
      request.put("instructions", INSTRUCTIONS + typeInstructions);
      request.put(
          "input",
          "다음 "
              + (briefingType == BriefingType.WEEKLY ? "주간" : "일일")
              + " 확정 원천데이터만 사용해 브리핑을 작성하라. briefingDate는 반드시 "
              + raw.baseDate()
              + "로 작성하라.\nRAW_DATA_JSON:\n"
              + raw.source().toString());
      request.put("store", false);
      request.put("max_output_tokens", 12000);
      request.putObject("reasoning").put("effort", "medium");
      ObjectNode format = request.putObject("text").putObject("format");
      format.put("type", "json_schema");
      format.put("name", "investment_briefing");
      format.put("strict", true);
      format.set("schema", json.readTree(SCHEMA_JSON));

      jdbc.sql(
              "UPDATE \"TB_BRF\" SET \"BRF_STS\"='GENERATING',\"AI_MODEL_NM\"=:model,\"UPD_USR_ID\"='OPENAI' WHERE \"BRF_ID\"=:id")
          .param("model", model)
          .param("id", raw.briefingId())
          .update();
      HttpRequest httpRequest =
          HttpRequest.newBuilder(responsesUri)
              .timeout(Duration.ofMinutes(3))
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(request)))
              .build();
      HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new IllegalStateException(
            "OpenAI API 오류 HTTP " + response.statusCode() + ": " + apiError(response.body()));
      JsonNode body = json.readTree(response.body());
      InvestmentBriefingResponse result =
          json.readValue(outputText(body), InvestmentBriefingResponse.class);
      if (!raw.baseDate().equals(result.briefingDate()))
        throw new IllegalStateException("OpenAI 응답 기준일이 원천데이터 기준일과 다릅니다.");
      if (!briefingType.name().equals(result.briefingType()))
        throw new IllegalStateException("OpenAI 응답 유형이 원천데이터 유형과 다릅니다.");
      JsonNode usage = body.path("usage");
      jdbc.sql(
              """
                    UPDATE "TB_BRF" SET "AI_MODEL_NM"=:model,"AI_REQ_ID"=:requestId,
                    "AI_INPUT_TOKEN_CNT"=:inputTokens,"AI_OUTPUT_TOKEN_CNT"=:outputTokens,
                    "AI_GEN_DTTM"=CURRENT_TIMESTAMP,"UPD_USR_ID"='OPENAI' WHERE "BRF_ID"=:id
                    """)
          .param("model", model)
          .param("requestId", body.path("id").asText(null))
          .param(
              "inputTokens",
              usage.path("input_tokens").isNumber() ? usage.path("input_tokens").asInt() : null)
          .param(
              "outputTokens",
              usage.path("output_tokens").isNumber() ? usage.path("output_tokens").asInt() : null)
          .param("id", raw.briefingId())
          .update();
      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OpenAI API 호출이 중단되었습니다.", e);
    } catch (Exception e) {
      if (e instanceof IllegalStateException state) throw state;
      throw new IllegalStateException("OpenAI 브리핑 생성에 실패했습니다.", e);
    }
  }

  private String outputText(JsonNode body) {
    for (JsonNode output : body.path("output"))
      for (JsonNode content : output.path("content"))
        if ("output_text".equals(content.path("type").asText())
            && StringUtils.hasText(content.path("text").asText()))
          return content.path("text").asText();
    throw new IllegalStateException("OpenAI 응답에 구조화된 output_text가 없습니다.");
  }

  private String apiError(String body) {
    try {
      String message = json.readTree(body).path("error").path("message").asText();
      return StringUtils.hasText(message) ? message : "응답 본문 없음";
    } catch (Exception ignored) {
      return "응답을 해석할 수 없음";
    }
  }
}
