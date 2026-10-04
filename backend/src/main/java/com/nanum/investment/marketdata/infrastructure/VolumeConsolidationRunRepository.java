package com.nanum.investment.marketdata.infrastructure;

import com.nanum.investment.marketdata.domain.VolumeConsolidationRun;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VolumeConsolidationRunRepository {
  private final JdbcClient jdbc;
  private static final RowMapper<VolumeConsolidationRun> MAPPER =
      (rs, n) ->
          new VolumeConsolidationRun(
              rs.getObject("RUN_ID", UUID.class),
              rs.getString("TRIGGER_CD"),
              rs.getObject("COLLECT_BASE_DT", LocalDate.class),
              rs.getObject("BASE_DT", LocalDate.class),
              rs.getString("RULE_VER"),
              rs.getString("STS"),
              rs.getObject("MATCH_CNT", Integer.class),
              rs.getObject("START_DTTM", OffsetDateTime.class),
              rs.getObject("END_DTTM", OffsetDateTime.class),
              rs.getString("FAIL_RSN"),
              rs.getBoolean("HAS_RESULT"));

  public VolumeConsolidationRunRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void start(UUID id, String trigger, LocalDate collected, String version) {
    jdbc.sql(
            """
        INSERT INTO "TB_VOL_CONSOL_RUN" ("RUN_ID","TRIGGER_CD","COLLECT_BASE_DT","RULE_VER","STS")
        VALUES (:id,:trigger,:collected,:version,'RUNNING')
        """)
        .param("id", id)
        .param("trigger", trigger)
        .param("collected", collected)
        .param("version", version)
        .update();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void finish(
      UUID id, String status, LocalDate baseDate, Integer matches, String result, String failure) {
    jdbc.sql(
            """
        UPDATE "TB_VOL_CONSOL_RUN" SET "STS"=:status, "BASE_DT"=:baseDate, "MATCH_CNT"=:matches,
          "RESULT_JSON"=CAST(:result AS jsonb), "FAIL_RSN"=:failure, "END_DTTM"=CURRENT_TIMESTAMP
        WHERE "RUN_ID"=:id
        """)
        .param("id", id)
        .param("status", status)
        .param("baseDate", baseDate)
        .param("matches", matches)
        .param("result", result)
        .param("failure", failure)
        .update();
  }

  public List<VolumeConsolidationRun> latest(int limit) {
    return jdbc.sql(
            """
        SELECT "RUN_ID","TRIGGER_CD","COLLECT_BASE_DT","BASE_DT","RULE_VER","STS","MATCH_CNT",
          "START_DTTM","END_DTTM","FAIL_RSN", "RESULT_JSON" IS NOT NULL AS "HAS_RESULT"
        FROM "TB_VOL_CONSOL_RUN" ORDER BY "START_DTTM" DESC, "RUN_ID" LIMIT :limit
        """)
        .param("limit", Math.clamp(limit, 1, 100))
        .query(MAPPER)
        .list();
  }

  public Optional<VolumeConsolidationRun> find(UUID id) {
    return jdbc.sql(
            """
        SELECT "RUN_ID","TRIGGER_CD","COLLECT_BASE_DT","BASE_DT","RULE_VER","STS","MATCH_CNT",
          "START_DTTM","END_DTTM","FAIL_RSN", "RESULT_JSON" IS NOT NULL AS "HAS_RESULT"
        FROM "TB_VOL_CONSOL_RUN" WHERE "RUN_ID"=:id
        """)
        .param("id", id)
        .query(MAPPER)
        .optional();
  }

  public Optional<String> result(UUID id) {
    return jdbc.sql(
            "SELECT \"RESULT_JSON\"::text FROM \"TB_VOL_CONSOL_RUN\" WHERE \"RUN_ID\"=:id AND \"RESULT_JSON\" IS NOT NULL")
        .param("id", id)
        .query(String.class)
        .optional();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int recoverInterrupted(OffsetDateTime applicationStartedAt) {
    return jdbc.sql(
            """
        UPDATE "TB_VOL_CONSOL_RUN" SET "STS"='FAILED', "END_DTTM"=CURRENT_TIMESTAMP,
          "FAIL_RSN"='애플리케이션 재시작 이전 검색이 완료되지 않았습니다.'
        WHERE "STS"='RUNNING' AND "START_DTTM" < :started
        """)
        .param("started", applicationStartedAt)
        .update();
  }
}
