package com.nanum.investment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.domain.BriefingType;
import com.nanum.investment.domain.DataStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WeeklyInvestmentBriefingService {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final JdbcClient jdbc;
  private final ObjectMapper json;
  private final TransactionTemplate transactions;
  private final InvestmentBriefingService briefingService;

  public WeeklyInvestmentBriefingService(
      JdbcClient jdbc,
      ObjectMapper json,
      TransactionTemplate transactions,
      InvestmentBriefingService briefingService) {
    this.jdbc = jdbc;
    this.json = json;
    this.transactions = transactions;
    this.briefingService = briefingService;
  }

  public Long generateAndSave() {
    return generateAndSave(LocalDate.now(KST));
  }

  public Long generateAndSave(LocalDate sunday) {
    if (sunday == null || sunday.getDayOfWeek() != DayOfWeek.SUNDAY)
      throw new IllegalArgumentException("주간 브리핑 기준일은 일요일이어야 합니다.");
    Long preparedId = transactions.execute(status -> prepareWeeklyRawData(sunday));
    Long publishedId = briefingService.generateAndSave(BriefingType.WEEKLY);
    if (!Objects.equals(preparedId, publishedId))
      throw new IllegalStateException("준비한 주간 원천데이터와 발행된 브리핑이 일치하지 않습니다.");
    return publishedId;
  }

  private Long prepareWeeklyRawData(LocalDate sunday) {
    LocalDate periodStart = sunday.minusDays(6);
    LocalDate periodEnd = sunday.minusDays(1);
    List<DailyRaw> daily =
        jdbc.sql(
                """
                SELECT "BASE_DT","RAW_DATA_JSON"::text,"DATA_STS","CONF_RT"
                FROM "TB_BRF"
                WHERE "BRF_TP"='DAILY' AND "SCOPE_TP"='GLOBAL' AND "LATEST_YN"='Y'
                  AND "BASE_DT" BETWEEN :start AND :end AND "RAW_DATA_JSON" IS NOT NULL
                  AND "BRF_STS" IN ('READY','GENERATING','GENERATED','REVIEWED','PUBLISHED','FAILED')
                ORDER BY "BASE_DT"
                """)
            .param("start", periodStart)
            .param("end", periodEnd)
            .query(
                (rs, n) ->
                    new DailyRaw(
                        rs.getObject(1, LocalDate.class),
                        rs.getString(2),
                        DataStatus.valueOf(rs.getString(3)),
                        rs.getBigDecimal(4)))
            .list();
    if (daily.isEmpty())
      throw new IllegalStateException(periodStart + "~" + periodEnd + " 일일 브리핑 원천데이터가 없습니다.");

    List<Map<String, Object>> days = new ArrayList<>();
    for (DailyRaw value : daily) {
      LinkedHashMap<String, Object> day = new LinkedHashMap<>();
      day.put("baseDate", value.baseDate());
      day.put("dataStatus", value.dataStatus().name());
      day.put("confidence", value.confidence());
      day.put("rawData", parseJson(value.rawJson()));
      days.add(day);
    }
    DataStatus dataStatus = worstStatus(daily);
    BigDecimal confidence =
        daily.stream()
            .map(DailyRaw::confidence)
            .filter(Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    LinkedHashMap<String, Object> raw = new LinkedHashMap<>();
    raw.put("schemaVersion", "weekly-1.0");
    raw.put("briefingType", "WEEKLY");
    raw.put("baseDate", sunday);
    raw.put("periodStart", periodStart);
    raw.put("periodEnd", periodEnd);
    raw.put("includedDailyBriefingCount", daily.size());
    raw.put("generatedAt", OffsetDateTime.now(KST));
    raw.put("dailyBriefings", days);

    jdbc.sql(
            """
                UPDATE "TB_BRF" SET "LATEST_YN"='N',"UPD_USR_ID"='SYSTEM'
                WHERE "BASE_DT"=:day AND "BRF_TP"='WEEKLY' AND "SCOPE_TP"='GLOBAL' AND "LATEST_YN"='Y'
                """)
        .param("day", sunday)
        .update();
    Integer sequence =
        jdbc.sql(
                """
                SELECT COALESCE(max("CALC_SEQ"),0)+1 FROM "TB_BRF"
                WHERE "BASE_DT"=:day AND "BRF_TP"='WEEKLY' AND "SCOPE_TP"='GLOBAL'
                """)
            .param("day", sunday)
            .query(Integer.class)
            .single();
    return jdbc.sql(
            """
                INSERT INTO "TB_BRF"("BASE_DT","CALC_SEQ","BRF_TP","SCOPE_TP","TITLE","BRF_STS",
                  "RAW_DATA_JSON","DATA_STS","CONF_RT","LATEST_YN","PUBL_YN","CRT_USR_ID","UPD_USR_ID")
                VALUES(:day,:sequence,'WEEKLY','GLOBAL',:title,'READY',CAST(:raw AS jsonb),:status,:confidence,
                  'Y','N','SYSTEM','SYSTEM') RETURNING "BRF_ID"
                """)
        .param("day", sunday)
        .param("sequence", sequence)
        .param("title", periodStart + "~" + periodEnd + " 주간 투자 브리핑")
        .param("raw", toJson(raw))
        .param("status", dataStatus.name())
        .param("confidence", confidence)
        .query(Long.class)
        .single();
  }

  private DataStatus worstStatus(List<DailyRaw> rows) {
    List<DataStatus> order =
        List.of(
            DataStatus.ERROR,
            DataStatus.MISSING,
            DataStatus.PARTIAL,
            DataStatus.STALE,
            DataStatus.FRESH);
    return order.stream()
        .filter(status -> rows.stream().anyMatch(row -> row.dataStatus() == status))
        .findFirst()
        .orElse(DataStatus.MISSING);
  }

  private JsonNode parseJson(String value) {
    try {
      return json.readTree(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("일일 브리핑 원천 JSON을 읽을 수 없습니다.", e);
    }
  }

  private String toJson(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("주간 브리핑 원천 JSON 생성에 실패했습니다.", e);
    }
  }

  private record DailyRaw(
      LocalDate baseDate, String rawJson, DataStatus dataStatus, BigDecimal confidence) {}
}
