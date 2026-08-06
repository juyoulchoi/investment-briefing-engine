package com.nanum.investment.service.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nanum.investment.domain.BriefingType;
import com.nanum.investment.response.InvestmentBriefingResponse;
import com.nanum.investment.service.BriefingAiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.*;
import java.time.*;

@Component
public class OpenAiBriefingAiClient implements BriefingAiClient {
    private static final String INSTRUCTIONS = """
            당신은 확정된 투자 계산 결과를 설명하는 한국어 투자 브리핑 작성자다.
            제공된 RAW_DATA_JSON의 숫자, 신호, 종목, 계좌를 변경하거나 새로 계산하지 마라.
            데이터에 없는 사실, 뉴스, 경제일정은 만들지 말고 '확정 데이터 없음'이라고 표시하라.
            사실과 해석을 구분하고 매수·매도 권유가 아닌 계산 결과 설명으로 작성하라.
            items에는 다음 14개 코드를 정확히 한 번씩 순서대로 작성하라:
            US_STOCK_MKT, US_BOND_MKT, KR_STOCK_MKT, FX_RATE_CMDTY, ECON_SCHEDULE, MKT_RISK,
            MKT_PHASE, REG_BUY_DEC, ADD_BUY_DEC, REBUY_SIG, ACCT_STRATEGY, HOLDING_SIGNAL,
            TODAY_ACTION, CAUTION.
            finalJudgment.itemCode는 반드시 FINAL_JUDGMENT여야 한다.
            signalCode는 NORMAL, WATCH, CAUTION, RISK 중 하나를 사용하라.
            """;
    private static final String SCHEMA_JSON = """
            {
              "type":"object",
              "additionalProperties":false,
              "required":["briefingDate","title","items","finalJudgment"],
              "properties":{
                "briefingDate":{"type":"string"},
                "title":{"type":"string"},
                "items":{"type":"array","minItems":14,"maxItems":14,"items":{"$ref":"#/$defs/item"}},
                "finalJudgment":{"$ref":"#/$defs/item"}
              },
              "$defs":{
                "item":{
                  "type":"object",
                  "additionalProperties":false,
                  "required":["itemCode","summary","content","signalCode","actionRequired"],
                  "properties":{
                    "itemCode":{"type":"string"},
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
    private final ObjectMapper json;
    private final HttpClient http;
    private final String apiKey;
    private final String model;
    private final URI responsesUri;

    @Autowired
    public OpenAiBriefingAiClient(
            JdbcClient jdbc,
            ObjectMapper json,
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-5.6-sol}") String model,
            @Value("${OPENAI_BASE_URL:https://api.openai.com/v1}") String baseUrl) {
        this(jdbc,json,apiKey,model,baseUrl,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    OpenAiBriefingAiClient(JdbcClient jdbc,ObjectMapper json,String apiKey,String model,String baseUrl,HttpClient http) {
        this.jdbc=jdbc;this.json=json;this.apiKey=apiKey;this.model=model;
        this.responsesUri=URI.create(baseUrl.replaceAll("/+$","")+"/responses");this.http=http;
    }

    @Override
    public InvestmentBriefingResponse generateBriefing(BriefingType briefingType) {
        if (!StringUtils.hasText(apiKey)) throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        if (briefingType == null) throw new IllegalArgumentException("브리핑 유형이 필요합니다.");
        RawBriefing raw=latestRawBriefing(briefingType);
        try {
            ObjectNode request=json.createObjectNode();
            String typeInstructions=briefingType==BriefingType.WEEKLY
                    ? " 지난 월요일부터 금요일까지의 변화, 반복 신호, 주간 요약을 중심으로 설명하라."
                    : " 해당 기준일의 일일 상황을 설명하라.";
            request.put("model",model);request.put("instructions",INSTRUCTIONS+typeInstructions);
            request.put("input","다음 "+(briefingType==BriefingType.WEEKLY?"주간":"일일")
                    +" 확정 원천데이터만 사용해 브리핑을 작성하라. briefingDate는 반드시 "+raw.baseDate()+"로 작성하라.\nRAW_DATA_JSON:\n"+raw.rawJson());
            request.put("store",false);request.put("max_output_tokens",12000);
            request.putObject("reasoning").put("effort","medium");
            ObjectNode format=request.putObject("text").putObject("format");
            format.put("type","json_schema");format.put("name","investment_briefing");format.put("strict",true);
            format.set("schema",json.readTree(SCHEMA_JSON));

            jdbc.sql("UPDATE \"TB_BRF\" SET \"BRF_STS\"='GENERATING',\"AI_MODEL_NM\"=:model,\"UPD_USR_ID\"='OPENAI' WHERE \"BRF_ID\"=:id")
                    .param("model",model).param("id",raw.briefingId()).update();
            HttpRequest httpRequest=HttpRequest.newBuilder(responsesUri).timeout(Duration.ofMinutes(3))
                    .header("Authorization","Bearer "+apiKey).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(request))).build();
            HttpResponse<String> response=http.send(httpRequest,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()<200||response.statusCode()>=300)
                throw new IllegalStateException("OpenAI API 오류 HTTP "+response.statusCode()+": "+apiError(response.body()));
            JsonNode body=json.readTree(response.body());
            InvestmentBriefingResponse result=json.readValue(outputText(body),InvestmentBriefingResponse.class);
            if(!raw.baseDate().equals(result.briefingDate()))
                throw new IllegalStateException("OpenAI 응답 기준일이 원천데이터 기준일과 다릅니다.");
            JsonNode usage=body.path("usage");
            jdbc.sql("""
                    UPDATE "TB_BRF" SET "AI_MODEL_NM"=:model,"AI_REQ_ID"=:requestId,
                    "AI_INPUT_TOKEN_CNT"=:inputTokens,"AI_OUTPUT_TOKEN_CNT"=:outputTokens,
                    "AI_GEN_DTTM"=CURRENT_TIMESTAMP,"UPD_USR_ID"='OPENAI' WHERE "BRF_ID"=:id
                    """).param("model",model).param("requestId",body.path("id").asText(null))
                    .param("inputTokens",usage.path("input_tokens").isNumber()?usage.path("input_tokens").asInt():null)
                    .param("outputTokens",usage.path("output_tokens").isNumber()?usage.path("output_tokens").asInt():null)
                    .param("id",raw.briefingId()).update();
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();throw new IllegalStateException("OpenAI API 호출이 중단되었습니다.",e);
        } catch (Exception e) {
            if(e instanceof IllegalStateException state)throw state;
            throw new IllegalStateException("OpenAI 브리핑 생성에 실패했습니다.",e);
        }
    }

    private RawBriefing latestRawBriefing(BriefingType briefingType){
        return jdbc.sql("""
                SELECT "BRF_ID","BASE_DT","RAW_DATA_JSON"::text FROM "TB_BRF"
                WHERE "LATEST_YN"='Y' AND "BRF_TP"=:briefingType AND "SCOPE_TP"='GLOBAL'
                  AND "RAW_DATA_JSON" IS NOT NULL AND "BRF_STS" IN ('READY','FAILED')
                ORDER BY "BASE_DT" DESC,"CALC_SEQ" DESC LIMIT 1
                """).param("briefingType",briefingType.name()).query((rs,n)->new RawBriefing(rs.getLong(1),rs.getObject(2,LocalDate.class),rs.getString(3)))
                .optional().orElseThrow(()->new IllegalStateException(briefingType+" 최신 브리핑 원천데이터가 없습니다."));
    }

    private String outputText(JsonNode body){
        for(JsonNode output:body.path("output"))for(JsonNode content:output.path("content"))
            if("output_text".equals(content.path("type").asText())&&StringUtils.hasText(content.path("text").asText()))
                return content.path("text").asText();
        throw new IllegalStateException("OpenAI 응답에 구조화된 output_text가 없습니다.");
    }

    private String apiError(String body){
        try{String message=json.readTree(body).path("error").path("message").asText();
            return StringUtils.hasText(message)?message:"응답 본문 없음";
        }catch(Exception ignored){return "응답을 해석할 수 없음";}
    }

    private record RawBriefing(Long briefingId,LocalDate baseDate,String rawJson){}
}
