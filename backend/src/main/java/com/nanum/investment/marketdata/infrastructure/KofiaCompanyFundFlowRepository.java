package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.marketdata.domain.KofiaCompanyFundFlowVariant;
import com.nanum.investment.marketdata.domain.KofiaCompanyFundFlowVariant.Stage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KofiaCompanyFundFlowRepository {
  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;

  public KofiaCompanyFundFlowRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  public List<CodeValue> activeCodes(String group) {
    return jdbc.sql(
            "SELECT \"CD_KEY\",\"CD_NM\" FROM \"TB_CD_DTL\" WHERE \"CD_GRP\"=:group AND \"ACTV_YN\"='Y' ORDER BY \"DSP_ORD\",\"CD_KEY\"")
        .param("group", group)
        .query((rs, n) -> new CodeValue(rs.getString(1), rs.getString(2)))
        .list();
  }

  @Transactional
  public int upsertVariants(List<VariantSeed> seeds) {
    int count = 0;
    for (VariantSeed seed : seeds) {
      count +=
          jdbc.sql(
                  """
              INSERT INTO "TB_KOFIA_CO_FUND_FLOW_VARIANT"("VARIANT_ID","STAGE_CD",
                "FUND_TYP_CD","FUND_KIND_CD","OFFERING_TYP_CD","ETF_INCLUDE_YN","PARAM_HASH")
              VALUES(:id,:stage,:fundType,:fundKind,:offering,:etf,:hash)
              ON CONFLICT("FUND_TYP_CD","FUND_KIND_CD","OFFERING_TYP_CD","ETF_INCLUDE_YN")
              DO UPDATE SET "STAGE_CD"=EXCLUDED."STAGE_CD","PARAM_HASH"=EXCLUDED."PARAM_HASH",
                "COLLECT_YN"='Y',"UPD_DTTM"=CURRENT_TIMESTAMP
              """)
              .param("id", UUID.randomUUID())
              .param("stage", seed.stage().name())
              .param("fundType", seed.fundTypeCode())
              .param("fundKind", seed.fundKindCode())
              .param("offering", seed.offeringTypeCode())
              .param("etf", seed.etfIncludeYn())
              .param("hash", seed.parameterHash())
              .update();
    }
    return count;
  }

  @Transactional
  public int syncGeneratedVariants(List<VariantSeed> seeds) {
    jdbc.sql(
            """
        UPDATE "TB_KOFIA_CO_FUND_FLOW_VARIANT" SET "COLLECT_YN"='N',"UPD_DTTM"=CURRENT_TIMESTAMP
        WHERE "STAGE_CD" IN ('AGGREGATE','OFFERING','FUND_TYPE','FUND_KIND')
        """)
        .update();
    return upsertVariants(seeds);
  }

  public List<KofiaCompanyFundFlowVariant> variants(Stage stage) {
    String sql =
        "SELECT * FROM \"TB_KOFIA_CO_FUND_FLOW_VARIANT\" WHERE \"COLLECT_YN\"='Y'"
            + (stage == Stage.ALL_ACTIVE ? "" : " AND \"STAGE_CD\"=:stage")
            + " ORDER BY \"STAGE_CD\",\"FUND_TYP_CD\",\"FUND_KIND_CD\",\"OFFERING_TYP_CD\",\"ETF_INCLUDE_YN\"";
    JdbcClient.StatementSpec query = jdbc.sql(sql);
    if (stage != Stage.ALL_ACTIVE) query = query.param("stage", stage.name());
    return query.query((rs, n) -> variant(rs)).list();
  }

  public List<Map<String, Object>> variantViews(Stage stage) {
    String sql =
        """
        SELECT "VARIANT_ID" variant_id,"STAGE_CD" stage_code,"FUND_TYP_CD" fund_type_code,
          "FUND_KIND_CD" fund_kind_code,"OFFERING_TYP_CD" offering_type_code,
          "ETF_INCLUDE_YN" etf_include_yn,"PARAM_HASH" parameter_hash,
          "COLLECT_YN" collect_yn,"FIRST_DATA_DT" first_data_date,
          "LAST_DATA_DT" last_data_date,"LAST_ROW_CNT" last_row_count,
          "EMPTY_STREAK_CNT" empty_streak_count
        FROM "TB_KOFIA_CO_FUND_FLOW_VARIANT" WHERE "COLLECT_YN"='Y'
        """
            + (stage == Stage.ALL_ACTIVE ? "" : " AND \"STAGE_CD\"=:stage")
            + " ORDER BY \"STAGE_CD\",\"FUND_TYP_CD\",\"FUND_KIND_CD\",\"OFFERING_TYP_CD\",\"ETF_INCLUDE_YN\"";
    JdbcClient.StatementSpec query = jdbc.sql(sql);
    if (stage != Stage.ALL_ACTIVE) query = query.param("stage", stage.name());
    return query.query().listOfRows();
  }

  @Transactional
  public synchronized void createJob(UUID jobId, Stage stage, LocalDate from, LocalDate to) {
    boolean overlap =
        jdbc.sql(
                """
            SELECT EXISTS(SELECT 1 FROM "TB_KOFIA_CO_FUND_FLOW_JOB"
            WHERE "STS" IN ('QUEUED','RUNNING') AND "FROM_DT"<=:to AND "TO_DT">=:from)
            """)
            .param("from", from)
            .param("to", to)
            .query(Boolean.class)
            .single();
    if (overlap) throw new IllegalStateException("기간이 겹치는 활성 회사별자금유출입 Job이 있습니다.");
    List<KofiaCompanyFundFlowVariant> variants = variants(stage);
    if (variants.isEmpty()) throw new IllegalStateException("수집할 활성 파라미터 조합이 없습니다: " + stage);
    List<LocalDate> dates = businessWeekdays(from, to);
    if (dates.isEmpty()) throw new IllegalStateException("수집 기간에 평일 기준일이 없습니다.");
    int total = Math.multiplyExact(variants.size(), dates.size());
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_CO_FUND_FLOW_JOB"("JOB_ID","STAGE_CD","FROM_DT","TO_DT",
          "TOTAL_ITEM_CNT") VALUES(:id,:stage,:from,:to,:total)
        """)
        .param("id", jobId)
        .param("stage", stage.name())
        .param("from", from)
        .param("to", to)
        .param("total", total)
        .update();
    for (KofiaCompanyFundFlowVariant variant : variants) {
      for (LocalDate date : dates) {
        jdbc.sql(
                """
            INSERT INTO "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM"("JOB_ID","VARIANT_ID","BASE_DT",
              "REQ_PARAMS") VALUES(:job,:variant,:date,CAST(:params AS jsonb))
            """)
            .param("job", jobId)
            .param("variant", variant.variantId())
            .param("date", date)
            .param("params", json(variant.requestParameters(date)))
            .update();
      }
    }
  }

  public boolean markRunning(UUID jobId) {
    return jdbc.sql(
                "UPDATE \"TB_KOFIA_CO_FUND_FLOW_JOB\" SET \"STS\"='RUNNING',\"START_DTTM\"=COALESCE(\"START_DTTM\",CURRENT_TIMESTAMP) WHERE \"JOB_ID\"=:id AND \"STS\" IN ('QUEUED','FAILED','COMPLETED_WITH_ERRORS')")
            .param("id", jobId)
            .update()
        > 0;
  }

  public PendingItem nextPending(UUID jobId) {
    return jdbc.sql(
            """
        SELECT i."ITEM_ID",i."JOB_ID",i."BASE_DT",v.*
        FROM "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM" i
        JOIN "TB_KOFIA_CO_FUND_FLOW_VARIANT" v ON v."VARIANT_ID"=i."VARIANT_ID"
        WHERE i."JOB_ID"=:job AND i."STS"='PENDING'
        ORDER BY i."BASE_DT",v."STAGE_CD",i."ITEM_ID" LIMIT 1
        """)
        .param("job", jobId)
        .query(
            (rs, n) ->
                new PendingItem(
                    rs.getLong("ITEM_ID"),
                    rs.getObject("JOB_ID", UUID.class),
                    variant(rs),
                    rs.getDate("BASE_DT").toLocalDate()))
        .optional()
        .orElse(null);
  }

  public void markItemRunning(long itemId) {
    jdbc.sql(
            "UPDATE \"TB_KOFIA_CO_FUND_FLOW_JOB_ITEM\" SET \"STS\"='RUNNING',\"START_DTTM\"=CURRENT_TIMESTAMP,\"ERROR_MSG\"=NULL WHERE \"ITEM_ID\"=:id")
        .param("id", itemId)
        .update();
  }

  @Transactional
  public SaveResult save(PendingItem item, KofiaClient.KofiaResponse response) {
    UUID rawId = UUID.randomUUID();
    String responseHash = KofiaSupport.sha256(response.rawResponse().toString());
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_CO_FUND_FLOW_RAW_RSP"("RAW_RSP_ID","JOB_ID","ITEM_ID",
          "VARIANT_ID","BASE_DT","REQ_PARAMS","ROW_CNT","RAW_HASH","PAYLOAD")
        VALUES(:raw,:job,:item,:variant,:date,CAST(:params AS jsonb),:count,:hash,
          CAST(:payload AS jsonb))
        """)
        .param("raw", rawId)
        .param("job", item.jobId())
        .param("item", item.itemId())
        .param("variant", item.variant().variantId())
        .param("date", item.baseDate())
        .param("params", json(response.requestParameters()))
        .param("count", response.rows().size())
        .param("hash", responseHash)
        .param("payload", response.rawResponse().toString())
        .update();
    int stored = 0;
    for (KofiaClient.KofiaRow row : response.rows()) {
      saveRow(item.variant(), row, rawId);
      stored++;
    }
    updateVariant(item.variant().variantId(), item.baseDate(), response.rows().size());
    return new SaveResult(response.rows().size(), stored, rawId, responseHash);
  }

  private void saveRow(
      KofiaCompanyFundFlowVariant variant, KofiaClient.KofiaRow row, UUID rawResponseId) {
    JsonNode payload = row.payload();
    String rowHash = KofiaSupport.sha256(payload.toString());
    jdbc.sql(
            """
        INSERT INTO "TB_KOFIA_CO_FUND_FLOW_ROW"("VARIANT_ID","BASE_DT","ROW_KEY",
          "COMPANY_TYP_CD","COMPANY_NM","FUND_TYP_CD","FUND_KIND_CD","OFFERING_TYP_CD",
          "ETF_INCLUDE_YN","METRIC_01_VAL","METRIC_02_VAL","METRIC_03_VAL",
          "METRIC_04_VAL","METRIC_05_VAL","METRIC_06_VAL","METRIC_07_VAL",
          "METRIC_08_VAL","METRIC_09_VAL","PERIOD_CD","RAW_RSP_ID","PAYLOAD","RAW_HASH")
        VALUES(:variant,:date,:rowKey,:companyType,:companyName,:fundType,:fundKind,:offering,
          :etf,:v2,:v3,:v4,:v5,:v6,:v7,:v8,:v9,:v10,:period,:raw,CAST(:payload AS jsonb),:hash)
        ON CONFLICT("VARIANT_ID","BASE_DT","ROW_KEY") DO UPDATE SET
          "COMPANY_TYP_CD"=EXCLUDED."COMPANY_TYP_CD","COMPANY_NM"=EXCLUDED."COMPANY_NM",
          "METRIC_01_VAL"=EXCLUDED."METRIC_01_VAL","METRIC_02_VAL"=EXCLUDED."METRIC_02_VAL",
          "METRIC_03_VAL"=EXCLUDED."METRIC_03_VAL","METRIC_04_VAL"=EXCLUDED."METRIC_04_VAL",
          "METRIC_05_VAL"=EXCLUDED."METRIC_05_VAL","METRIC_06_VAL"=EXCLUDED."METRIC_06_VAL",
          "METRIC_07_VAL"=EXCLUDED."METRIC_07_VAL","METRIC_08_VAL"=EXCLUDED."METRIC_08_VAL",
          "METRIC_09_VAL"=EXCLUDED."METRIC_09_VAL","PERIOD_CD"=EXCLUDED."PERIOD_CD",
          "RAW_RSP_ID"=EXCLUDED."RAW_RSP_ID","PAYLOAD"=EXCLUDED."PAYLOAD",
          "RAW_HASH"=EXCLUDED."RAW_HASH","LAST_COLLECT_DTTM"=CURRENT_TIMESTAMP,
          "UPD_DTTM"=CASE WHEN "TB_KOFIA_CO_FUND_FLOW_ROW"."RAW_HASH"<>EXCLUDED."RAW_HASH"
            THEN CURRENT_TIMESTAMP ELSE "TB_KOFIA_CO_FUND_FLOW_ROW"."UPD_DTTM" END
        """)
        .param("variant", variant.variantId())
        .param("date", row.baseDate())
        .param("rowKey", row.rowKey())
        .param("companyType", payload.path("TMPV98").asText(null))
        .param("companyName", payload.path("TMPV1").asText(null))
        .param("fundType", variant.fundTypeCode())
        .param("fundKind", variant.fundKindCode())
        .param("offering", variant.offeringTypeCode())
        .param("etf", variant.etfIncludeYn())
        .param("v2", decimal(payload, "TMPV2"))
        .param("v3", decimal(payload, "TMPV3"))
        .param("v4", decimal(payload, "TMPV4"))
        .param("v5", decimal(payload, "TMPV5"))
        .param("v6", decimal(payload, "TMPV6"))
        .param("v7", decimal(payload, "TMPV7"))
        .param("v8", decimal(payload, "TMPV8"))
        .param("v9", decimal(payload, "TMPV9"))
        .param("v10", decimal(payload, "TMPV10"))
        .param("period", payload.path("TMPV99").asText(null))
        .param("raw", rawResponseId)
        .param("payload", payload.toString())
        .param("hash", rowHash)
        .update();
  }

  private void updateVariant(UUID variantId, LocalDate baseDate, int rowCount) {
    if (rowCount == 0) {
      jdbc.sql(
              "UPDATE \"TB_KOFIA_CO_FUND_FLOW_VARIANT\" SET \"LAST_ROW_CNT\"=0,\"EMPTY_STREAK_CNT\"=\"EMPTY_STREAK_CNT\"+1,\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"VARIANT_ID\"=:id")
          .param("id", variantId)
          .update();
      return;
    }
    jdbc.sql(
            """
        UPDATE "TB_KOFIA_CO_FUND_FLOW_VARIANT" SET
          "FIRST_DATA_DT"=LEAST(COALESCE("FIRST_DATA_DT",:date),:date),
          "LAST_DATA_DT"=GREATEST(COALESCE("LAST_DATA_DT",:date),:date),
          "LAST_ROW_CNT"=:count,"EMPTY_STREAK_CNT"=0,"UPD_DTTM"=CURRENT_TIMESTAMP
        WHERE "VARIANT_ID"=:id
        """)
        .param("date", baseDate)
        .param("count", rowCount)
        .param("id", variantId)
        .update();
  }

  public void finishItem(long itemId, SaveResult result) {
    jdbc.sql(
            """
        UPDATE "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM" SET "STS"=:status,
          "RECEIVED_CNT"=:received,"STORED_CNT"=:stored,"COMPLETE_DTTM"=CURRENT_TIMESTAMP
        WHERE "ITEM_ID"=:id
        """)
        .param("status", result.receivedCount() == 0 ? "SUCCESS_EMPTY" : "SUCCESS")
        .param("received", result.receivedCount())
        .param("stored", result.storedCount())
        .param("id", itemId)
        .update();
  }

  public void failItem(long itemId, String error) {
    jdbc.sql(
            "UPDATE \"TB_KOFIA_CO_FUND_FLOW_JOB_ITEM\" SET \"STS\"='FAILED',\"ERROR_MSG\"=:error,\"COMPLETE_DTTM\"=CURRENT_TIMESTAMP WHERE \"ITEM_ID\"=:id")
        .param("id", itemId)
        .param("error", KofiaSupport.trim(error))
        .update();
  }

  public void complete(UUID jobId) {
    jdbc.sql(
            """
        UPDATE "TB_KOFIA_CO_FUND_FLOW_JOB" j SET
          "SUCCESS_ITEM_CNT"=(SELECT count(*) FROM "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM"
            WHERE "JOB_ID"=j."JOB_ID" AND "STS"='SUCCESS'),
          "EMPTY_ITEM_CNT"=(SELECT count(*) FROM "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM"
            WHERE "JOB_ID"=j."JOB_ID" AND "STS"='SUCCESS_EMPTY'),
          "FAILED_ITEM_CNT"=(SELECT count(*) FROM "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM"
            WHERE "JOB_ID"=j."JOB_ID" AND "STS"='FAILED'),
          "STS"=CASE WHEN EXISTS(SELECT 1 FROM "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM"
            WHERE "JOB_ID"=j."JOB_ID" AND "STS"='FAILED')
            THEN 'COMPLETED_WITH_ERRORS' ELSE 'COMPLETED' END,
          "COMPLETE_DTTM"=CURRENT_TIMESTAMP WHERE "JOB_ID"=:id
        """)
        .param("id", jobId)
        .update();
  }

  public void failJob(UUID jobId, String error) {
    jdbc.sql(
            "UPDATE \"TB_KOFIA_CO_FUND_FLOW_JOB\" SET \"STS\"='FAILED',\"ERROR_MSG\"=:error,\"COMPLETE_DTTM\"=CURRENT_TIMESTAMP WHERE \"JOB_ID\"=:id")
        .param("id", jobId)
        .param("error", KofiaSupport.trim(error))
        .update();
  }

  public int retryFailures(UUID jobId) {
    int count =
        jdbc.sql(
                """
            UPDATE "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM" SET "STS"='PENDING',
              "RETRY_CNT"="RETRY_CNT"+1,"ERROR_MSG"=NULL,"START_DTTM"=NULL,
              "COMPLETE_DTTM"=NULL WHERE "JOB_ID"=:id AND "STS"='FAILED'
            """)
            .param("id", jobId)
            .update();
    if (count > 0)
      jdbc.sql(
              "UPDATE \"TB_KOFIA_CO_FUND_FLOW_JOB\" SET \"STS\"='QUEUED',\"ERROR_MSG\"=NULL,\"COMPLETE_DTTM\"=NULL WHERE \"JOB_ID\"=:id")
          .param("id", jobId)
          .update();
    return count;
  }

  @Transactional
  public List<UUID> recoverQueuedJobs() {
    jdbc.sql(
            """
        UPDATE "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM" SET "STS"='PENDING',
          "RETRY_CNT"="RETRY_CNT"+1,"ERROR_MSG"='애플리케이션 재시작 후 자동 복구',
          "START_DTTM"=NULL,"COMPLETE_DTTM"=NULL WHERE "STS"='RUNNING'
        """)
        .update();
    jdbc.sql(
            """
        UPDATE "TB_KOFIA_CO_FUND_FLOW_JOB" SET "STS"='QUEUED',
          "ERROR_MSG"='애플리케이션 재시작 후 자동 복구',"COMPLETE_DTTM"=NULL
        WHERE "STS"='RUNNING'
        """)
        .update();
    return jdbc.sql(
            "SELECT \"JOB_ID\" FROM \"TB_KOFIA_CO_FUND_FLOW_JOB\" WHERE \"STS\"='QUEUED' ORDER BY \"CRT_DTTM\"")
        .query(UUID.class)
        .list();
  }

  public JobView job(UUID jobId, boolean includeItems) {
    JobView base =
        jdbc.sql("SELECT * FROM \"TB_KOFIA_CO_FUND_FLOW_JOB\" WHERE \"JOB_ID\"=:id")
            .param("id", jobId)
            .query(
                (rs, n) ->
                    new JobView(
                        rs.getObject("JOB_ID", UUID.class),
                        rs.getString("STAGE_CD"),
                        rs.getDate("FROM_DT").toLocalDate(),
                        rs.getDate("TO_DT").toLocalDate(),
                        rs.getString("STS"),
                        rs.getInt("TOTAL_ITEM_CNT"),
                        rs.getInt("SUCCESS_ITEM_CNT"),
                        rs.getInt("EMPTY_ITEM_CNT"),
                        rs.getInt("FAILED_ITEM_CNT"),
                        rs.getString("ERROR_MSG"),
                        time(rs.getTimestamp("CRT_DTTM")),
                        time(rs.getTimestamp("START_DTTM")),
                        time(rs.getTimestamp("COMPLETE_DTTM")),
                        List.of()))
            .optional()
            .orElseThrow(() -> new NoSuchElementException("회사별자금유출입 Job을 찾을 수 없습니다: " + jobId));
    if (!includeItems) return base;
    List<Map<String, Object>> items =
        jdbc.sql(
                """
            SELECT "ITEM_ID" item_id,"VARIANT_ID" variant_id,"BASE_DT" base_date,
              "REQ_PARAMS" request_parameters,"STS" status,"RECEIVED_CNT" received_count,
              "STORED_CNT" stored_count,"RETRY_CNT" retry_count,"ERROR_MSG" error_message,
              "START_DTTM" started_at,"COMPLETE_DTTM" completed_at
            FROM "TB_KOFIA_CO_FUND_FLOW_JOB_ITEM" WHERE "JOB_ID"=:id
            ORDER BY "BASE_DT","ITEM_ID"
            """)
            .param("id", jobId)
            .query()
            .listOfRows();
    return base.withItems(items);
  }

  public List<JobView> jobs(int limit) {
    return jdbc
        .sql(
            "SELECT \"JOB_ID\" FROM \"TB_KOFIA_CO_FUND_FLOW_JOB\" ORDER BY \"CRT_DTTM\" DESC LIMIT :limit")
        .param("limit", Math.min(Math.max(limit, 1), 100))
        .query(UUID.class)
        .list()
        .stream()
        .map(id -> job(id, false))
        .toList();
  }

  public List<Map<String, Object>> rows(LocalDate from, LocalDate to, Stage stage, int limit) {
    return jdbc.sql(
            """
        SELECT r."BASE_DT" base_date,v."STAGE_CD" stage_code,r."ROW_KEY" row_key,
          r."COMPANY_TYP_CD" company_type_code,r."COMPANY_NM" company_name,
          r."FUND_TYP_CD" fund_type_code,r."FUND_KIND_CD" fund_kind_code,
          r."OFFERING_TYP_CD" offering_type_code,r."ETF_INCLUDE_YN" etf_include_yn,
          r."METRIC_01_VAL" metric_01_value,r."METRIC_02_VAL" metric_02_value,
          r."METRIC_03_VAL" metric_03_value,r."METRIC_04_VAL" metric_04_value,
          r."METRIC_05_VAL" metric_05_value,r."METRIC_06_VAL" metric_06_value,
          r."METRIC_07_VAL" metric_07_value,r."METRIC_08_VAL" metric_08_value,
          r."METRIC_09_VAL" metric_09_value,r."PERIOD_CD" period_code,
          r."PAYLOAD" payload,r."LAST_COLLECT_DTTM" last_collected_at
        FROM "TB_KOFIA_CO_FUND_FLOW_ROW" r
        JOIN "TB_KOFIA_CO_FUND_FLOW_VARIANT" v ON v."VARIANT_ID"=r."VARIANT_ID"
        WHERE r."BASE_DT" BETWEEN :from AND :to AND (:allStages OR v."STAGE_CD"=:stage)
        ORDER BY r."BASE_DT" DESC,v."STAGE_CD",r."FUND_TYP_CD",r."FUND_KIND_CD",
          r."OFFERING_TYP_CD",r."ETF_INCLUDE_YN",r."ROW_KEY" LIMIT :limit
        """)
        .param("from", from)
        .param("to", to)
        .param("allStages", stage == Stage.ALL_ACTIVE)
        .param("stage", stage.name())
        .param("limit", Math.min(Math.max(limit, 1), 10000))
        .query()
        .listOfRows();
  }

  private KofiaCompanyFundFlowVariant variant(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new KofiaCompanyFundFlowVariant(
        rs.getObject("VARIANT_ID", UUID.class),
        Stage.valueOf(rs.getString("STAGE_CD")),
        rs.getString("FUND_TYP_CD"),
        rs.getString("FUND_KIND_CD"),
        rs.getString("OFFERING_TYP_CD"),
        rs.getString("ETF_INCLUDE_YN"),
        rs.getString("PARAM_HASH"));
  }

  static List<LocalDate> businessWeekdays(LocalDate from, LocalDate to) {
    List<LocalDate> dates = new ArrayList<>();
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1))
      if (date.getDayOfWeek().getValue() < 6) dates.add(date);
    return dates;
  }

  private BigDecimal decimal(JsonNode payload, String field) {
    JsonNode value = payload.path(field);
    if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) return null;
    return new BigDecimal(value.asText().replace(",", ""));
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception error) {
      throw new IllegalStateException("회사별자금유출입 요청조건 JSON 변환에 실패했습니다.", error);
    }
  }

  private LocalDateTime time(java.sql.Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }

  public record CodeValue(String code, String name) {}

  public record VariantSeed(
      Stage stage,
      String fundTypeCode,
      String fundKindCode,
      String offeringTypeCode,
      String etfIncludeYn,
      String parameterHash) {}

  public record PendingItem(
      long itemId, UUID jobId, KofiaCompanyFundFlowVariant variant, LocalDate baseDate) {}

  public record SaveResult(
      int receivedCount, int storedCount, UUID rawResponseId, String responseHash) {}

  public record JobView(
      UUID jobId,
      String stage,
      LocalDate from,
      LocalDate to,
      String status,
      int totalItemCount,
      int successItemCount,
      int emptyItemCount,
      int failedItemCount,
      String error,
      LocalDateTime createdAt,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      List<Map<String, Object>> items) {
    JobView withItems(List<Map<String, Object>> values) {
      return new JobView(
          jobId,
          stage,
          from,
          to,
          status,
          totalItemCount,
          successItemCount,
          emptyItemCount,
          failedItemCount,
          error,
          createdAt,
          startedAt,
          completedAt,
          values);
    }
  }
}
