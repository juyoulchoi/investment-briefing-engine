package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.databind.*;
import com.nanum.investment.briefing.domain.BriefingType;
import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class BriefingSnapshotService {
  private final JdbcClient jdbc;
  private final ObjectMapper json;
  public BriefingSnapshotService(JdbcClient jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

  public FixedBriefingSnapshot latest(BriefingType type) {
    return jdbc.sql("""
        SELECT "BRF_ID","BASE_DT","BRF_TP","RAW_DATA_JSON"::text,"SOURCE_FIX_DTTM"
        FROM "TB_BRF" WHERE "LATEST_YN"='Y' AND "BRF_TP"=:type AND "SCOPE_TP"='GLOBAL'
          AND "RAW_DATA_JSON" IS NOT NULL AND "SOURCE_FIX_DTTM" IS NOT NULL
          AND "BRF_STS" IN ('READY','FAILED')
        ORDER BY "BASE_DT" DESC,"CALC_SEQ" DESC LIMIT 1
        """).param("type", type.name()).query((rs,n) -> {
          JsonNode source = parse(rs.getString(4));
          if (!source.path("confirmedValues").isObject())
            throw new IllegalStateException("DB 확정값이 없는 브리핑 Snapshot은 OpenAI에 전달할 수 없습니다.");
          return new FixedBriefingSnapshot(rs.getLong(1), rs.getObject(2, LocalDate.class),
              BriefingType.valueOf(rs.getString(3)), source);
        }).optional().orElseThrow(() -> new IllegalStateException(type + " 최신 확정 브리핑 Snapshot이 없습니다."));
  }
  private JsonNode parse(String value) { try { return json.readTree(value); } catch (Exception e) { throw new IllegalStateException("브리핑 Snapshot JSON을 읽을 수 없습니다.", e); } }
  public record FixedBriefingSnapshot(Long briefingId, LocalDate baseDate, BriefingType briefingType, JsonNode source) {}
}
