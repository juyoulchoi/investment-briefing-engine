package com.nanum.investment.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class FredMacroRepository {
  private final JdbcClient jdbc;

  public FredMacroRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<SeriesView> series(boolean activeOnly) {
    return jdbc.sql(
            """
            SELECT "SERIES_ID","SERIES_CD","SERIES_NM","CATEGORY_CD","CNTRY_CD","FREQUENCY_CD",
                   "UNIT_NM","SEASONAL_ADJ_NM","OBS_START_DT","OBS_END_DT","FRED_LAST_UPD_DTTM",
                   "TRANSFORM_CD","AGGREGATION_CD","COLLECT_CYCLE_CD","REFRESH_OVERLAP_DAY",
                   "VINTAGE_POLICY_CD","TARGET_CD","LAST_SUCCESS_DT","LATEST_OBS_DT","USE_YN"
              FROM "TB_FRED_SERIES"
             WHERE (:activeOnly=false OR "USE_YN"='Y')
             ORDER BY "CATEGORY_CD","SERIES_CD"
            """)
        .param("activeOnly", activeOnly)
        .query((rs, n) -> seriesView(rs))
        .list();
  }

  public SeriesView series(String code) {
    return jdbc.sql(
            """
            SELECT "SERIES_ID","SERIES_CD","SERIES_NM","CATEGORY_CD","CNTRY_CD","FREQUENCY_CD",
                   "UNIT_NM","SEASONAL_ADJ_NM","OBS_START_DT","OBS_END_DT","FRED_LAST_UPD_DTTM",
                   "TRANSFORM_CD","AGGREGATION_CD","COLLECT_CYCLE_CD","REFRESH_OVERLAP_DAY",
                   "VINTAGE_POLICY_CD","TARGET_CD","LAST_SUCCESS_DT","LATEST_OBS_DT","USE_YN"
              FROM "TB_FRED_SERIES" WHERE "SERIES_CD"=:code
            """)
        .param("code", code)
        .query((rs, n) -> seriesView(rs))
        .optional()
        .orElseThrow(() -> new NoSuchElementException("FRED Series를 찾을 수 없습니다: " + code));
  }

  @Transactional
  public SeriesView upsertSeries(FredClient.SeriesMetadata metadata, SeriesCommand command) {
    jdbc.sql(
            """
            INSERT INTO "TB_FRED_SERIES"(
              "SERIES_CD","SERIES_NM","CATEGORY_CD","CNTRY_CD","FREQUENCY_CD","UNIT_NM",
              "SEASONAL_ADJ_NM","OBS_START_DT","OBS_END_DT","FRED_LAST_UPD_DTTM","TRANSFORM_CD",
              "AGGREGATION_CD","COLLECT_CYCLE_CD","REFRESH_OVERLAP_DAY","VINTAGE_POLICY_CD","TARGET_CD","USE_YN")
            VALUES(:code,:name,:category,:country,:frequency,:units,:seasonal,:obsStart,:obsEnd,:lastUpdated,
                   :transform,:aggregation,:cycle,:overlap,:vintage,:target,:useYn)
            ON CONFLICT("SERIES_CD") DO UPDATE SET
              "SERIES_NM"=EXCLUDED."SERIES_NM","CATEGORY_CD"=EXCLUDED."CATEGORY_CD",
              "CNTRY_CD"=EXCLUDED."CNTRY_CD","FREQUENCY_CD"=EXCLUDED."FREQUENCY_CD",
              "UNIT_NM"=EXCLUDED."UNIT_NM","SEASONAL_ADJ_NM"=EXCLUDED."SEASONAL_ADJ_NM",
              "OBS_START_DT"=EXCLUDED."OBS_START_DT","OBS_END_DT"=EXCLUDED."OBS_END_DT",
              "FRED_LAST_UPD_DTTM"=EXCLUDED."FRED_LAST_UPD_DTTM","TRANSFORM_CD"=EXCLUDED."TRANSFORM_CD",
              "AGGREGATION_CD"=EXCLUDED."AGGREGATION_CD","COLLECT_CYCLE_CD"=EXCLUDED."COLLECT_CYCLE_CD",
              "REFRESH_OVERLAP_DAY"=EXCLUDED."REFRESH_OVERLAP_DAY","VINTAGE_POLICY_CD"=EXCLUDED."VINTAGE_POLICY_CD",
              "TARGET_CD"=EXCLUDED."TARGET_CD","USE_YN"=EXCLUDED."USE_YN","UPD_DTTM"=CURRENT_TIMESTAMP
            """)
        .param("code", metadata.seriesCode())
        .param("name", value(command.displayName(), metadata.title()))
        .param("category", value(command.categoryCode(), "MACRO"))
        .param("country", value(command.countryCode(), "US"))
        .param("frequency", metadata.frequencyShort())
        .param("units", metadata.units())
        .param("seasonal", metadata.seasonalAdjustment())
        .param("obsStart", metadata.observationStart())
        .param("obsEnd", metadata.observationEnd())
        .param("lastUpdated", metadata.lastUpdated())
        .param("transform", value(command.transformCode(), "lin"))
        .param("aggregation", value(command.aggregationCode(), "avg"))
        .param("cycle", value(command.collectionCycleCode(), "DAILY"))
        .param("overlap", command.refreshOverlapDays() == null ? 120 : command.refreshOverlapDays())
        .param("vintage", value(command.vintagePolicyCode(), "LATEST_ONLY"))
        .param("target", command.targetCode())
        .param("useYn", value(command.useYn(), "Y"))
        .update();
    return series(metadata.seriesCode());
  }

  public void setActive(String code, boolean active) {
    int count =
        jdbc.sql(
                "UPDATE \"TB_FRED_SERIES\" SET \"USE_YN\"=:yn,\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"SERIES_CD\"=:code")
            .param("yn", active ? "Y" : "N")
            .param("code", code)
            .update();
    if (count == 0) throw new NoSuchElementException("FRED Series를 찾을 수 없습니다: " + code);
  }

  public List<Map<String, Object>> observations(String code, LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            SELECT s."SERIES_CD" series_code,s."SERIES_NM" series_name,o."OBS_DT" observation_date,
                   o."OBS_VAL" observation_value,o."VALUE_STS_CD" value_status,
                   o."REALTIME_START_DT" realtime_start,o."REALTIME_END_DT" realtime_end,
                   o."DATA_STS" data_status,o."LAST_COLLECT_DTTM" last_collected_at
              FROM "TB_FRED_OBS" o JOIN "TB_FRED_SERIES" s ON s."SERIES_ID"=o."SERIES_ID"
             WHERE s."SERIES_CD"=:code AND o."OBS_DT" BETWEEN :from AND :to
             ORDER BY o."OBS_DT"
            """)
        .param("code", code)
        .param("from", from)
        .param("to", to)
        .query()
        .listOfRows();
  }

  public List<Map<String, Object>> revisions(String code, LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
            SELECT s."SERIES_CD" series_code,h."OBS_DT" observation_date,h."REVISION_SEQ" revision_sequence,
                   h."OLD_OBS_VAL" old_value,h."NEW_OBS_VAL" new_value,h."CHANGE_TYPE_CD" change_type,
                   h."REALTIME_START_DT" realtime_start,h."REALTIME_END_DT" realtime_end,
                   h."JOB_ID" job_id,h."DETECTED_DTTM" detected_at
              FROM "TB_FRED_OBS_HIST" h JOIN "TB_FRED_SERIES" s ON s."SERIES_ID"=h."SERIES_ID"
             WHERE s."SERIES_CD"=:code AND h."OBS_DT" BETWEEN :from AND :to
             ORDER BY h."OBS_DT",h."REVISION_SEQ"
            """)
        .param("code", code)
        .param("from", from)
        .param("to", to)
        .query()
        .listOfRows();
  }

  @Transactional
  public SaveResult saveObservation(
      long seriesId, UUID jobId, FredClient.Observation observation, String hash) {
    Existing existing =
        jdbc.sql(
                "SELECT \"OBS_VAL\",\"VALUE_STS_CD\" FROM \"TB_FRED_OBS\" WHERE \"SERIES_ID\"=:id AND \"OBS_DT\"=:day")
            .param("id", seriesId)
            .param("day", observation.observationDate())
            .query(
                (rs, n) -> new Existing(rs.getBigDecimal("OBS_VAL"), rs.getString("VALUE_STS_CD")))
            .optional()
            .orElse(null);
    String status = observation.value() == null ? "MISSING" : "VALUE";
    if (existing == null) {
      jdbc.sql(
              """
              INSERT INTO "TB_FRED_OBS"("SERIES_ID","OBS_DT","OBS_VAL","VALUE_STS_CD","REALTIME_START_DT",
                "REALTIME_END_DT","RAW_HASH","FIRST_JOB_ID","LAST_JOB_ID","DATA_STS")
              VALUES(:id,:day,:value,:status,:realStart,:realEnd,:hash,:job,:job,:dataStatus)
              """)
          .param("id", seriesId)
          .param("day", observation.observationDate())
          .param("value", observation.value())
          .param("status", status)
          .param("realStart", observation.realtimeStart())
          .param("realEnd", observation.realtimeEnd())
          .param("hash", hash)
          .param("job", jobId)
          .param("dataStatus", observation.value() == null ? "MISSING" : "FRESH")
          .update();
      return observation.value() == null ? SaveResult.MISSING_INSERTED : SaveResult.INSERTED;
    }
    boolean changed = !Objects.equals(existing.value(), observation.value());
    if (changed) {
      Integer sequence =
          jdbc.sql(
                  "SELECT COALESCE(MAX(\"REVISION_SEQ\"),0)+1 FROM \"TB_FRED_OBS_HIST\" WHERE \"SERIES_ID\"=:id AND \"OBS_DT\"=:day")
              .param("id", seriesId)
              .param("day", observation.observationDate())
              .query(Integer.class)
              .single();
      jdbc.sql(
              """
              INSERT INTO "TB_FRED_OBS_HIST"("SERIES_ID","OBS_DT","REVISION_SEQ","OLD_OBS_VAL","NEW_OBS_VAL",
                "CHANGE_TYPE_CD","REALTIME_START_DT","REALTIME_END_DT","JOB_ID")
              VALUES(:id,:day,:sequence,:oldValue,:newValue,:changeType,:realStart,:realEnd,:job)
              """)
          .param("id", seriesId)
          .param("day", observation.observationDate())
          .param("sequence", sequence)
          .param("oldValue", existing.value())
          .param("newValue", observation.value())
          .param("changeType", changeType(existing.value(), observation.value()))
          .param("realStart", observation.realtimeStart())
          .param("realEnd", observation.realtimeEnd())
          .param("job", jobId)
          .update();
    }
    jdbc.sql(
            """
            UPDATE "TB_FRED_OBS" SET "OBS_VAL"=:value,"VALUE_STS_CD"=:status,"REALTIME_START_DT"=:realStart,
              "REALTIME_END_DT"=:realEnd,"RAW_HASH"=:hash,"LAST_JOB_ID"=:job,
              "LAST_COLLECT_DTTM"=CURRENT_TIMESTAMP,"DATA_STS"=:dataStatus
             WHERE "SERIES_ID"=:id AND "OBS_DT"=:day
            """)
        .param("value", observation.value())
        .param("status", changed && observation.value() != null ? "REVISED" : status)
        .param("realStart", observation.realtimeStart())
        .param("realEnd", observation.realtimeEnd())
        .param("hash", hash)
        .param("job", jobId)
        .param("dataStatus", observation.value() == null ? "MISSING" : "FRESH")
        .param("id", seriesId)
        .param("day", observation.observationDate())
        .update();
    if (changed)
      return observation.value() == null ? SaveResult.MISSING_UPDATED : SaveResult.UPDATED;
    return observation.value() == null ? SaveResult.MISSING_UNCHANGED : SaveResult.UNCHANGED;
  }

  public void updateSeriesSuccess(long seriesId, LocalDate latest) {
    jdbc.sql(
            "UPDATE \"TB_FRED_SERIES\" SET \"LAST_SUCCESS_DT\"=:day,\"LATEST_OBS_DT\"=GREATEST(\"LATEST_OBS_DT\",:day),\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"SERIES_ID\"=:id")
        .param("day", latest)
        .param("id", seriesId)
        .update();
  }

  @Transactional
  public void createJob(
      UUID id, LocalDate from, LocalDate to, List<SeriesView> series, long interval) {
    String codes =
        series.stream()
            .map(SeriesView::seriesCode)
            .sorted()
            .reduce((a, b) -> a + "," + b)
            .orElseThrow();
    jdbc.sql(
            """
            INSERT INTO "TB_FRED_CLCT_JOB"("JOB_ID","FROM_DT","TO_DT","STATUS","SERIES_CDS","REQUEST_INTERVAL_MS","TOTAL_ITEM_CNT","PENDING_ITEM_CNT")
            VALUES(:id,:from,:to,'QUEUED',:codes,:interval,:total,:total)
            """)
        .param("id", id)
        .param("from", from)
        .param("to", to)
        .param("codes", codes)
        .param("interval", interval)
        .param("total", series.size())
        .update();
    for (SeriesView value : series)
      jdbc.sql(
              "INSERT INTO \"TB_FRED_CLCT_JOB_ITEM\"(\"JOB_ID\",\"SERIES_ID\") VALUES(:job,:series)")
          .param("job", id)
          .param("series", value.seriesId())
          .update();
  }

  public boolean hasActiveOverlap(LocalDate from, LocalDate to, String codes) {
    return jdbc.sql(
            """
            SELECT EXISTS(SELECT 1 FROM "TB_FRED_CLCT_JOB"
             WHERE "STATUS" IN ('QUEUED','RUNNING','PAUSE_REQUESTED','PAUSED','CANCEL_REQUESTED')
               AND "SERIES_CDS"=:codes AND "FROM_DT"<=:to AND "TO_DT">=:from)
            """)
        .param("codes", codes)
        .param("from", from)
        .param("to", to)
        .query(Boolean.class)
        .single();
  }

  public boolean markRunning(UUID id) {
    return jdbc.sql(
                "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"='RUNNING',\"START_DTTM\"=COALESCE(\"START_DTTM\",CURRENT_TIMESTAMP),\"END_DTTM\"=NULL WHERE \"JOB_ID\"=:id AND \"STATUS\"='QUEUED'")
            .param("id", id)
            .update()
        == 1;
  }

  public String jobStatus(UUID id) {
    return jdbc.sql("SELECT \"STATUS\" FROM \"TB_FRED_CLCT_JOB\" WHERE \"JOB_ID\"=:id")
        .param("id", id)
        .query(String.class)
        .optional()
        .orElseThrow();
  }

  public Optional<PendingItem> nextPending(UUID id) {
    return jdbc.sql(
            """
            SELECT i."JOB_ITEM_ID",s."SERIES_ID",s."SERIES_CD",s."TRANSFORM_CD",s."AGGREGATION_CD",
                   j."FROM_DT",j."TO_DT",j."REQUEST_INTERVAL_MS"
              FROM "TB_FRED_CLCT_JOB_ITEM" i
              JOIN "TB_FRED_SERIES" s ON s."SERIES_ID"=i."SERIES_ID"
              JOIN "TB_FRED_CLCT_JOB" j ON j."JOB_ID"=i."JOB_ID"
             WHERE i."JOB_ID"=:id AND i."STATUS"='PENDING' ORDER BY i."JOB_ITEM_ID" LIMIT 1
            """)
        .param("id", id)
        .query(
            (rs, n) ->
                new PendingItem(
                    rs.getLong("JOB_ITEM_ID"),
                    rs.getLong("SERIES_ID"),
                    rs.getString("SERIES_CD"),
                    rs.getString("TRANSFORM_CD"),
                    rs.getString("AGGREGATION_CD"),
                    rs.getObject("FROM_DT", LocalDate.class),
                    rs.getObject("TO_DT", LocalDate.class),
                    rs.getLong("REQUEST_INTERVAL_MS")))
        .optional();
  }

  public void markItemRunning(long id) {
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB_ITEM\" SET \"STATUS\"='RUNNING',\"ATTEMPT_CNT\"=\"ATTEMPT_CNT\"+1,\"START_DTTM\"=CURRENT_TIMESTAMP,\"END_DTTM\"=NULL,\"ERROR_MSG\"=NULL WHERE \"JOB_ITEM_ID\"=:id")
        .param("id", id)
        .update();
  }

  public void finishItem(long id, MacroCollectionResult result) {
    jdbc.sql(
            """
            UPDATE "TB_FRED_CLCT_JOB_ITEM" SET "STATUS"='SUCCESS',"RECEIVED_CNT"=:received,
              "INSERTED_CNT"=:inserted,"UPDATED_CNT"=:updated,"UNCHANGED_CNT"=:unchanged,
              "MISSING_CNT"=:missing,"REVISION_CNT"=:revisions,"END_DTTM"=CURRENT_TIMESTAMP
             WHERE "JOB_ITEM_ID"=:id
            """)
        .param("received", result.receivedCount())
        .param("inserted", result.insertedCount())
        .param("updated", result.updatedCount())
        .param("unchanged", result.unchangedCount())
        .param("missing", result.missingCount())
        .param("revisions", result.revisionCount())
        .param("id", id)
        .update();
  }

  public void failItem(long id, String error) {
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB_ITEM\" SET \"STATUS\"='FAILED',\"ERROR_MSG\"=:error,\"END_DTTM\"=CURRENT_TIMESTAMP WHERE \"JOB_ITEM_ID\"=:id")
        .param("error", trim(error))
        .param("id", id)
        .update();
  }

  public void updateProgress(UUID id) {
    jdbc.sql(
            """
            UPDATE "TB_FRED_CLCT_JOB" j SET
              "SUCCESS_ITEM_CNT"=(SELECT COUNT(*) FROM "TB_FRED_CLCT_JOB_ITEM" WHERE "JOB_ID"=j."JOB_ID" AND "STATUS"='SUCCESS'),
              "FAILED_ITEM_CNT"=(SELECT COUNT(*) FROM "TB_FRED_CLCT_JOB_ITEM" WHERE "JOB_ID"=j."JOB_ID" AND "STATUS"='FAILED'),
              "PENDING_ITEM_CNT"=(SELECT COUNT(*) FROM "TB_FRED_CLCT_JOB_ITEM" WHERE "JOB_ID"=j."JOB_ID" AND "STATUS" IN ('PENDING','RUNNING'))
             WHERE j."JOB_ID"=:id
            """)
        .param("id", id)
        .update();
  }

  public void complete(UUID id) {
    updateProgress(id);
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"=CASE WHEN \"FAILED_ITEM_CNT\">0 THEN 'COMPLETED_WITH_ERRORS' ELSE 'COMPLETED' END,\"END_DTTM\"=CURRENT_TIMESTAMP WHERE \"JOB_ID\"=:id")
        .param("id", id)
        .update();
  }

  public void failJob(UUID id, String error) {
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"='FAILED',\"ERROR_MSG\"=:error,\"END_DTTM\"=CURRENT_TIMESTAMP WHERE \"JOB_ID\"=:id")
        .param("error", trim(error))
        .param("id", id)
        .update();
  }

  public void requestPause(UUID id) {
    transition(id, "PAUSE_REQUESTED", List.of("QUEUED", "RUNNING"));
  }

  public void markPaused(UUID id) {
    transition(id, "PAUSED", List.of("PAUSE_REQUESTED"));
  }

  public void requestCancel(UUID id) {
    transition(id, "CANCEL_REQUESTED", List.of("QUEUED", "RUNNING", "PAUSE_REQUESTED", "PAUSED"));
  }

  public void cancel(UUID id) {
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB_ITEM\" SET \"STATUS\"='CANCELLED',\"END_DTTM\"=CURRENT_TIMESTAMP WHERE \"JOB_ID\"=:id AND \"STATUS\"='PENDING'")
        .param("id", id)
        .update();
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"='CANCELLED',\"END_DTTM\"=CURRENT_TIMESTAMP WHERE \"JOB_ID\"=:id")
        .param("id", id)
        .update();
  }

  public void resume(UUID id) {
    transition(id, "QUEUED", List.of("PAUSED"));
  }

  public int retryFailures(UUID id) {
    int count =
        jdbc.sql(
                "UPDATE \"TB_FRED_CLCT_JOB_ITEM\" SET \"STATUS\"='PENDING',\"ERROR_MSG\"=NULL,\"END_DTTM\"=NULL WHERE \"JOB_ID\"=:id AND \"STATUS\"='FAILED'")
            .param("id", id)
            .update();
    if (count > 0)
      jdbc.sql(
              "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"='QUEUED',\"ERROR_MSG\"=NULL,\"END_DTTM\"=NULL WHERE \"JOB_ID\"=:id")
          .param("id", id)
          .update();
    return count;
  }

  @Transactional
  public List<UUID> recoverInterrupted() {
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB_ITEM\" SET \"STATUS\"='PENDING',\"START_DTTM\"=NULL WHERE \"STATUS\"='RUNNING'")
        .update();
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"='PAUSED' WHERE \"STATUS\"='PAUSE_REQUESTED'")
        .update();
    jdbc.sql(
            "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"='CANCELLED',\"END_DTTM\"=CURRENT_TIMESTAMP WHERE \"STATUS\"='CANCEL_REQUESTED'")
        .update();
    jdbc.sql("UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"='QUEUED' WHERE \"STATUS\"='RUNNING'")
        .update();
    return jdbc.sql(
            "SELECT \"JOB_ID\" FROM \"TB_FRED_CLCT_JOB\" WHERE \"STATUS\"='QUEUED' ORDER BY \"CRT_DTTM\"")
        .query(UUID.class)
        .list();
  }

  public JobView job(UUID id) {
    List<ItemView> items = items(id);
    return jdbc.sql(
            "SELECT \"JOB_ID\",\"FROM_DT\",\"TO_DT\",\"STATUS\",\"SERIES_CDS\",\"REQUEST_INTERVAL_MS\",\"TOTAL_ITEM_CNT\",\"SUCCESS_ITEM_CNT\",\"FAILED_ITEM_CNT\",\"PENDING_ITEM_CNT\",\"ERROR_MSG\",\"CRT_DTTM\",\"START_DTTM\",\"END_DTTM\" FROM \"TB_FRED_CLCT_JOB\" WHERE \"JOB_ID\"=:id")
        .param("id", id)
        .query(
            (rs, n) ->
                new JobView(
                    (UUID) rs.getObject("JOB_ID"),
                    rs.getObject("FROM_DT", LocalDate.class),
                    rs.getObject("TO_DT", LocalDate.class),
                    rs.getString("STATUS"),
                    List.of(rs.getString("SERIES_CDS").split(",")),
                    rs.getLong("REQUEST_INTERVAL_MS"),
                    rs.getInt("TOTAL_ITEM_CNT"),
                    rs.getInt("SUCCESS_ITEM_CNT"),
                    rs.getInt("FAILED_ITEM_CNT"),
                    rs.getInt("PENDING_ITEM_CNT"),
                    rs.getString("ERROR_MSG"),
                    time(rs.getTimestamp("CRT_DTTM")),
                    time(rs.getTimestamp("START_DTTM")),
                    time(rs.getTimestamp("END_DTTM")),
                    items))
        .optional()
        .orElseThrow(() -> new NoSuchElementException("FRED 수집 Job을 찾을 수 없습니다: " + id));
  }

  public List<JobView> jobs(int limit) {
    return jdbc
        .sql("SELECT \"JOB_ID\" FROM \"TB_FRED_CLCT_JOB\" ORDER BY \"CRT_DTTM\" DESC LIMIT :limit")
        .param("limit", Math.max(1, Math.min(limit, 100)))
        .query(UUID.class)
        .list()
        .stream()
        .map(this::job)
        .toList();
  }

  private List<ItemView> items(UUID id) {
    return jdbc.sql(
            """
            SELECT i."JOB_ITEM_ID",s."SERIES_CD",i."STATUS",i."RECEIVED_CNT",i."INSERTED_CNT",i."UPDATED_CNT",
              i."UNCHANGED_CNT",i."MISSING_CNT",i."REVISION_CNT",i."ATTEMPT_CNT",i."ERROR_MSG",i."START_DTTM",i."END_DTTM"
            FROM "TB_FRED_CLCT_JOB_ITEM" i JOIN "TB_FRED_SERIES" s ON s."SERIES_ID"=i."SERIES_ID"
            WHERE i."JOB_ID"=:id ORDER BY i."JOB_ITEM_ID"
            """)
        .param("id", id)
        .query(
            (rs, n) ->
                new ItemView(
                    rs.getLong("JOB_ITEM_ID"),
                    rs.getString("SERIES_CD"),
                    rs.getString("STATUS"),
                    rs.getInt("RECEIVED_CNT"),
                    rs.getInt("INSERTED_CNT"),
                    rs.getInt("UPDATED_CNT"),
                    rs.getInt("UNCHANGED_CNT"),
                    rs.getInt("MISSING_CNT"),
                    rs.getInt("REVISION_CNT"),
                    rs.getInt("ATTEMPT_CNT"),
                    rs.getString("ERROR_MSG"),
                    time(rs.getTimestamp("START_DTTM")),
                    time(rs.getTimestamp("END_DTTM"))))
        .list();
  }

  private void transition(UUID id, String target, List<String> sources) {
    int count =
        jdbc.sql(
                "UPDATE \"TB_FRED_CLCT_JOB\" SET \"STATUS\"=:target WHERE \"JOB_ID\"=:id AND \"STATUS\" IN (:sources)")
            .param("target", target)
            .param("id", id)
            .param("sources", sources)
            .update();
    if (count == 0)
      throw new IllegalStateException("현재 상태에서는 FRED Job 상태를 " + target + "(으)로 변경할 수 없습니다.");
  }

  private SeriesView seriesView(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new SeriesView(
        rs.getLong("SERIES_ID"),
        rs.getString("SERIES_CD"),
        rs.getString("SERIES_NM"),
        rs.getString("CATEGORY_CD"),
        rs.getString("CNTRY_CD"),
        rs.getString("FREQUENCY_CD"),
        rs.getString("UNIT_NM"),
        rs.getString("SEASONAL_ADJ_NM"),
        rs.getObject("OBS_START_DT", LocalDate.class),
        rs.getObject("OBS_END_DT", LocalDate.class),
        rs.getObject("FRED_LAST_UPD_DTTM", OffsetDateTime.class),
        rs.getString("TRANSFORM_CD"),
        rs.getString("AGGREGATION_CD"),
        rs.getString("COLLECT_CYCLE_CD"),
        rs.getInt("REFRESH_OVERLAP_DAY"),
        rs.getString("VINTAGE_POLICY_CD"),
        rs.getString("TARGET_CD"),
        rs.getObject("LAST_SUCCESS_DT", LocalDate.class),
        rs.getObject("LATEST_OBS_DT", LocalDate.class),
        "Y".equals(rs.getString("USE_YN")));
  }

  private String value(String requested, String fallback) {
    return requested == null || requested.isBlank() ? fallback : requested.trim();
  }

  private String changeType(BigDecimal oldValue, BigDecimal newValue) {
    if (oldValue == null) return "MISSING_TO_VALUE";
    if (newValue == null) return "VALUE_TO_MISSING";
    return "UPDATE";
  }

  private String trim(String value) {
    return value == null || value.length() <= 4000 ? value : value.substring(0, 4000);
  }

  private LocalDateTime time(Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }

  private record Existing(BigDecimal value, String status) {}

  public enum SaveResult {
    INSERTED,
    UPDATED,
    UNCHANGED,
    MISSING_INSERTED,
    MISSING_UPDATED,
    MISSING_UNCHANGED
  }

  public record SeriesCommand(
      String displayName,
      String categoryCode,
      String countryCode,
      String transformCode,
      String aggregationCode,
      String collectionCycleCode,
      Integer refreshOverlapDays,
      String vintagePolicyCode,
      String targetCode,
      String useYn) {}

  public record SeriesView(
      long seriesId,
      String seriesCode,
      String seriesName,
      String categoryCode,
      String countryCode,
      String frequencyCode,
      String units,
      String seasonalAdjustment,
      LocalDate observationStart,
      LocalDate observationEnd,
      OffsetDateTime fredLastUpdated,
      String transformCode,
      String aggregationCode,
      String collectionCycleCode,
      int refreshOverlapDays,
      String vintagePolicyCode,
      String targetCode,
      LocalDate lastSuccessDate,
      LocalDate latestObservationDate,
      boolean active) {}

  public record PendingItem(
      long itemId,
      long seriesId,
      String seriesCode,
      String transformCode,
      String aggregationCode,
      LocalDate from,
      LocalDate to,
      long requestIntervalMillis) {}

  public record MacroCollectionResult(
      String seriesCode,
      LocalDate from,
      LocalDate to,
      int receivedCount,
      int insertedCount,
      int updatedCount,
      int unchangedCount,
      int missingCount,
      int revisionCount,
      LocalDate latestObservationDate) {}

  public record ItemView(
      long itemId,
      String seriesCode,
      String status,
      int receivedCount,
      int insertedCount,
      int updatedCount,
      int unchangedCount,
      int missingCount,
      int revisionCount,
      int attemptCount,
      String error,
      LocalDateTime startedAt,
      LocalDateTime completedAt) {}

  public record JobView(
      UUID jobId,
      LocalDate from,
      LocalDate to,
      String status,
      List<String> seriesCodes,
      long requestIntervalMillis,
      int totalItemCount,
      int successItemCount,
      int failedItemCount,
      int pendingItemCount,
      String error,
      LocalDateTime createdAt,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      List<ItemView> items) {
    @JsonProperty("progressRate")
    public double progressRate() {
      return totalItemCount == 0
          ? 100
          : Math.round((successItemCount + failedItemCount) * 10000.0 / totalItemCount) / 100.0;
    }
  }
}
