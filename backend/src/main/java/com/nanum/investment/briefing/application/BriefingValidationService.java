package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.databind.*;
import com.nanum.investment.briefing.api.response.*;
import com.nanum.investment.briefing.domain.BriefingType;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BriefingValidationService {
  private static final Logger log = LoggerFactory.getLogger(BriefingValidationService.class);
  private final JdbcClient jdbc;
  private final ObjectMapper json;
  public BriefingValidationService(JdbcClient jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

  public void validate(Long briefingId, BriefingType expectedType, InvestmentBriefingResponse response) {
    try {
      Source source = jdbc.sql("SELECT \"BASE_DT\",\"BRF_TP\",\"RAW_DATA_JSON\"->'confirmedValues' FROM \"TB_BRF\" WHERE \"BRF_ID\"=:id")
          .param("id", briefingId).query((rs,n) -> new Source(rs.getObject(1, LocalDate.class), rs.getString(2), parse(rs.getString(3))))
          .optional().orElseThrow(() -> new IllegalStateException("검증할 DB 브리핑 Snapshot이 없습니다."));
      require(response != null && response.briefingDate() != null && StringUtils.hasText(response.title()), "브리핑 기준일과 제목이 필요합니다.");
      require(source.date().equals(response.briefingDate()), "DB 기준일과 OpenAI 결과 기준일이 다릅니다.");
      require(expectedType.name().equals(source.type()) && expectedType.name().equals(response.briefingType()), "DAILY/WEEKLY 타입이 다릅니다.");
      require(response.items() != null && response.items().size() == BriefingItemCatalog.ITEMS.size(), "브리핑은 15개 항목이어야 합니다.");
      List<String> expectedCodes = BriefingItemCatalog.ITEMS.stream().map(BriefingItemCatalog.Item::code).toList();
      List<String> actualCodes = response.items().stream().map(BriefingItemResponse::itemCode).toList();
      require(expectedCodes.equals(actualCodes), "브리핑 항목 코드 또는 순서가 기준과 다릅니다: " + actualCodes);
      for (int i = 0; i < response.items().size(); i++) {
        BriefingItemResponse item = response.items().get(i);
        require(BriefingItemCatalog.ITEMS.get(i).title().equals(item.title()), item.itemCode()+" 항목 제목이 기준과 다릅니다.");
        require(StringUtils.hasText(item.summary()) && StringUtils.hasText(item.content()) && StringUtils.hasText(item.signalCode()), item.itemCode()+" 항목 설명이 없습니다.");
        require(Set.of("NORMAL","WATCH","CAUTION","RISK").contains(item.signalCode()), item.itemCode()+" 신호 코드가 잘못되었습니다.");
      }
      JsonNode confirmed = response.confirmedValues();
      require(confirmed != null && source.confirmedValues().equals(confirmed), "DB 확정 숫자·등급·신호와 OpenAI 결과가 다릅니다.");
      int risk = confirmed.path("marketRiskScore").asInt(-1), direction = confirmed.path("marketDirectionScore").asInt(-1);
      require(risk >= 0 && risk <= 100, "시장 위험지수 범위가 잘못되었습니다.");
      require(direction >= 0 && direction <= 100, "시장방향예측지수 범위가 잘못되었습니다.");
      int sum = 0;
      for (String key : List.of("uptrendResume","boxRange","reCorrection","retestLow")) {
        int probability = confirmed.path(key).asInt(-1);
        require(probability >= 0 && probability <= 100, key+" 확률 범위가 잘못되었습니다.");
        sum += probability;
      }
      require(sum == 100, "4개 시나리오 확률 합계가 100이 아닙니다.");
    } catch (RuntimeException e) {
      log.error("브리핑 발행 검증 실패. briefingId={}, type={}, reason={}", briefingId, expectedType, e.getMessage());
      jdbc.sql("UPDATE \"TB_BRF\" SET \"BRF_STS\"='FAILED',\"FAIL_RSN\"=:reason,\"PUBL_YN\"='N',\"UPD_USR_ID\"='VALIDATOR' WHERE \"BRF_ID\"=:id")
          .param("reason", Objects.toString(e.getMessage(), e.getClass().getSimpleName())).param("id", briefingId).update();
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("브리핑 검증 중 DB Snapshot을 읽지 못했습니다.", e);
    }
  }
  private void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
  private JsonNode parse(String value) {
    try { return json.readTree(value); }
    catch (Exception e) { throw new IllegalStateException("DB 확정 JSON을 읽을 수 없습니다.", e); }
  }
  private record Source(LocalDate date, String type, JsonNode confirmedValues) {}
}
