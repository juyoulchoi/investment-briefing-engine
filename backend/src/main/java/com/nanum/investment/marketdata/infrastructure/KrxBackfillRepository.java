package com.nanum.investment.marketdata.infrastructure;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class KrxBackfillRepository {
  private final JdbcClient jdbc;

  public KrxBackfillRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public void create(
      UUID id,
      LocalDate from,
      LocalDate to,
      String datasetCodes,
      long requestIntervalMillis,
      List<NewDay> days) {
    jdbc.sql(
            """
            INSERT INTO tb_krx_bf_job(id,from_date,to_date,status,dataset_codes,
              request_interval_ms,total_day_count,pending_day_count,skipped_day_count)
            VALUES (:id,:from,:to,'QUEUED',:datasets,:interval,:total,:pending,:skipped)
            """)
        .param("id", id)
        .param("from", from)
        .param("to", to)
        .param("datasets", datasetCodes)
        .param("interval", requestIntervalMillis)
        .param("total", days.size())
        .param("pending", days.stream().filter(day -> "PENDING".equals(day.status())).count())
        .param("skipped", days.stream().filter(day -> "SKIPPED".equals(day.status())).count())
        .update();
    for (NewDay day : days)
      jdbc.sql(
              """
              INSERT INTO tb_krx_bf_day(backfill_job_id,base_date,status,skip_reason)
              VALUES (:id,:date,:status,:reason)
              """)
          .param("id", id)
          .param("date", day.date())
          .param("status", day.status())
          .param("reason", day.reason())
          .update();
  }

  public boolean hasActiveOverlap(LocalDate from, LocalDate to, String datasetCodes) {
    return jdbc.sql(
            """
            SELECT EXISTS (
              SELECT 1 FROM tb_krx_bf_job
              WHERE status IN ('QUEUED','RUNNING','PAUSE_REQUESTED','PAUSED','CANCEL_REQUESTED')
                AND dataset_codes=:datasets AND from_date<=:to AND to_date>=:from)
            """)
        .param("from", from)
        .param("to", to)
        .param("datasets", datasetCodes)
        .query(Boolean.class)
        .single();
  }

  public Optional<Boolean> marketOpen(LocalDate date) {
    return jdbc.sql("SELECT open_yn='Y' FROM tb_mkt_cal WHERE cal_dt=:date AND market_cd='KRX'")
        .param("date", date)
        .query(Boolean.class)
        .optional();
  }

  public BackfillJobView find(UUID id) {
    BackfillJobView header =
        jdbc.sql("SELECT * FROM tb_krx_bf_job WHERE id=:id")
            .param("id", id)
            .query((rs, row) -> mapJob(rs, List.of()))
            .optional()
            .orElseThrow(() -> new NoSuchElementException("KRX 백필 Job을 찾을 수 없습니다: " + id));
    List<BackfillDayView> days =
        jdbc.sql("SELECT * FROM tb_krx_bf_day WHERE backfill_job_id=:id ORDER BY base_date")
            .param("id", id)
            .query(
                (rs, row) ->
                    new BackfillDayView(
                        rs.getLong("id"),
                        rs.getObject("base_date", LocalDate.class),
                        rs.getString("status"),
                        rs.getString("skip_reason"),
                        rs.getObject("collection_job_id", UUID.class),
                        split(rs.getString("retry_dataset_codes")),
                        rs.getInt("attempt_count"),
                        rs.getString("error_message"),
                        time(rs.getTimestamp("started_at")),
                        time(rs.getTimestamp("completed_at"))))
            .list();
    return new BackfillJobView(
        header.jobId(),
        header.from(),
        header.to(),
        header.status(),
        header.datasets(),
        header.requestIntervalMillis(),
        header.totalDayCount(),
        header.successDayCount(),
        header.failedDayCount(),
        header.skippedDayCount(),
        header.pendingDayCount(),
        header.currentDate(),
        header.error(),
        header.createdAt(),
        header.startedAt(),
        header.completedAt(),
        days);
  }

  public List<BackfillJobView> findAll(int limit) {
    return jdbc.sql("SELECT * FROM tb_krx_bf_job ORDER BY created_at DESC LIMIT :limit")
        .param("limit", Math.max(1, Math.min(limit, 100)))
        .query((rs, row) -> mapJob(rs, List.of()))
        .list();
  }

  public String status(UUID id) {
    return jdbc.sql("SELECT status FROM tb_krx_bf_job WHERE id=:id")
        .param("id", id)
        .query(String.class)
        .optional()
        .orElseThrow(() -> new NoSuchElementException("KRX 백필 Job을 찾을 수 없습니다: " + id));
  }

  public boolean markRunning(UUID id) {
    return jdbc.sql(
                """
            UPDATE tb_krx_bf_job SET status='RUNNING',started_at=COALESCE(started_at,CURRENT_TIMESTAMP),
              completed_at=NULL,error_message=NULL,updated_at=CURRENT_TIMESTAMP
            WHERE id=:id AND status='QUEUED'
            """)
            .param("id", id)
            .update()
        == 1;
  }

  public Optional<BackfillDayView> nextPending(UUID id) {
    return jdbc.sql(
            "SELECT * FROM tb_krx_bf_day WHERE backfill_job_id=:id AND status='PENDING' ORDER BY base_date LIMIT 1")
        .param("id", id)
        .query(
            (rs, row) ->
                new BackfillDayView(
                    rs.getLong("id"),
                    rs.getObject("base_date", LocalDate.class),
                    rs.getString("status"),
                    rs.getString("skip_reason"),
                    rs.getObject("collection_job_id", UUID.class),
                    split(rs.getString("retry_dataset_codes")),
                    rs.getInt("attempt_count"),
                    rs.getString("error_message"),
                    time(rs.getTimestamp("started_at")),
                    time(rs.getTimestamp("completed_at"))))
        .optional();
  }

  public void markDayRunning(long dayId, UUID collectionJobId) {
    jdbc.sql(
            """
            UPDATE tb_krx_bf_day SET status='RUNNING',collection_job_id=:collectionJobId,
              attempt_count=attempt_count+1,error_message=NULL,started_at=CURRENT_TIMESTAMP,
              completed_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=:id
            """)
        .param("id", dayId)
        .param("collectionJobId", collectionJobId)
        .update();
  }

  public void finishDay(long dayId, boolean success, String error) {
    jdbc.sql(
            """
            UPDATE tb_krx_bf_day SET status=:status,error_message=:error,retry_dataset_codes=NULL,
              completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=:id
            """)
        .param("id", dayId)
        .param("status", success ? "SUCCESS" : "FAILED")
        .param("error", trim(error))
        .update();
  }

  public void updateProgress(UUID id, LocalDate currentDate) {
    jdbc.sql(
            """
            UPDATE tb_krx_bf_job j SET
              success_day_count=(SELECT count(*) FROM tb_krx_bf_day d WHERE d.backfill_job_id=j.id AND d.status='SUCCESS'),
              failed_day_count=(SELECT count(*) FROM tb_krx_bf_day d WHERE d.backfill_job_id=j.id AND d.status='FAILED'),
              skipped_day_count=(SELECT count(*) FROM tb_krx_bf_day d WHERE d.backfill_job_id=j.id AND d.status='SKIPPED'),
              pending_day_count=(SELECT count(*) FROM tb_krx_bf_day d WHERE d.backfill_job_id=j.id AND d.status='PENDING'),
              current_base_date=:date,updated_at=CURRENT_TIMESTAMP WHERE id=:id
            """)
        .param("id", id)
        .param("date", currentDate)
        .update();
  }

  public void complete(UUID id) {
    updateProgress(id, null);
    jdbc.sql(
            """
            UPDATE tb_krx_bf_job SET
              status=CASE WHEN failed_day_count>0 THEN 'COMPLETED_WITH_ERRORS' ELSE 'COMPLETED' END,
              current_base_date=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=:id
            """)
        .param("id", id)
        .update();
  }

  public void fail(UUID id, String error) {
    jdbc.sql(
            """
            UPDATE tb_krx_bf_job SET status='FAILED',error_message=:error,current_base_date=NULL,
              completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=:id
            """)
        .param("id", id)
        .param("error", trim(error))
        .update();
  }

  public void requestPause(UUID id) {
    int count =
        jdbc.sql(
                "UPDATE tb_krx_bf_job SET status='PAUSE_REQUESTED',updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='RUNNING'")
            .param("id", id)
            .update();
    if (count == 0) throw new IllegalStateException("RUNNING 상태의 백필 Job만 일시중지할 수 있습니다.");
  }

  public void markPaused(UUID id) {
    jdbc.sql(
            "UPDATE tb_krx_bf_job SET status='PAUSED',current_base_date=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=:id")
        .param("id", id)
        .update();
  }

  public void queueResume(UUID id) {
    int count =
        jdbc.sql(
                "UPDATE tb_krx_bf_job SET status='QUEUED',completed_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=:id AND status='PAUSED'")
            .param("id", id)
            .update();
    if (count == 0) throw new IllegalStateException("PAUSED 상태의 백필 Job만 재개할 수 있습니다.");
  }

  public void requestCancel(UUID id) {
    String current = status(id);
    if ("RUNNING".equals(current) || "PAUSE_REQUESTED".equals(current)) {
      jdbc.sql(
              "UPDATE tb_krx_bf_job SET status='CANCEL_REQUESTED',updated_at=CURRENT_TIMESTAMP WHERE id=:id")
          .param("id", id)
          .update();
    } else if ("QUEUED".equals(current) || "PAUSED".equals(current)) {
      cancel(id);
    } else throw new IllegalStateException("현재 상태에서는 백필 Job을 취소할 수 없습니다: " + current);
  }

  public void cancel(UUID id) {
    jdbc.sql(
            "UPDATE tb_krx_bf_day SET status='CANCELLED',completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE backfill_job_id=:id AND status='PENDING'")
        .param("id", id)
        .update();
    jdbc.sql(
            "UPDATE tb_krx_bf_job SET status='CANCELLED',current_base_date=NULL,completed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=:id")
        .param("id", id)
        .update();
    updateProgress(id, null);
  }

  @Transactional
  public int resetFailures(UUID id, boolean datasetOnly, KrxCollectionJobRepository dailyJobs) {
    List<Map<String, Object>> failures =
        jdbc.sql(
                "SELECT id,collection_job_id FROM tb_krx_bf_day WHERE backfill_job_id=:id AND status='FAILED'")
            .param("id", id)
            .query()
            .listOfRows();
    for (Map<String, Object> row : failures) {
      UUID collectionJobId = (UUID) row.get("collection_job_id");
      String retryDatasets =
          datasetOnly && collectionJobId != null
              ? String.join(",", dailyJobs.failedDatasets(collectionJobId))
              : null;
      jdbc.sql(
              """
              UPDATE tb_krx_bf_day SET status='PENDING',retry_dataset_codes=:datasets,
                error_message=NULL,completed_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=:dayId
              """)
          .param("dayId", ((Number) row.get("id")).longValue())
          .param("datasets", retryDatasets)
          .update();
    }
    if (!failures.isEmpty()) {
      jdbc.sql(
              "UPDATE tb_krx_bf_job SET status='QUEUED',completed_at=NULL,error_message=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=:id")
          .param("id", id)
          .update();
      updateProgress(id, null);
    }
    return failures.size();
  }

  private BackfillJobView mapJob(java.sql.ResultSet rs, List<BackfillDayView> days)
      throws java.sql.SQLException {
    return new BackfillJobView(
        rs.getObject("id", UUID.class),
        rs.getObject("from_date", LocalDate.class),
        rs.getObject("to_date", LocalDate.class),
        rs.getString("status"),
        split(rs.getString("dataset_codes")),
        rs.getLong("request_interval_ms"),
        rs.getInt("total_day_count"),
        rs.getInt("success_day_count"),
        rs.getInt("failed_day_count"),
        rs.getInt("skipped_day_count"),
        rs.getInt("pending_day_count"),
        rs.getObject("current_base_date", LocalDate.class),
        rs.getString("error_message"),
        time(rs.getTimestamp("created_at")),
        time(rs.getTimestamp("started_at")),
        time(rs.getTimestamp("completed_at")),
        days);
  }

  private static List<String> split(String value) {
    return value == null || value.isBlank() ? List.of() : List.of(value.split(","));
  }

  private static LocalDateTime time(Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }

  private static String trim(String value) {
    if (value == null || value.length() <= 4000) return value;
    return value.substring(0, 4000);
  }

  public record NewDay(LocalDate date, String status, String reason) {}

  public record BackfillJobView(
      UUID jobId,
      LocalDate from,
      LocalDate to,
      String status,
      List<String> datasets,
      long requestIntervalMillis,
      int totalDayCount,
      int successDayCount,
      int failedDayCount,
      int skippedDayCount,
      int pendingDayCount,
      LocalDate currentDate,
      String error,
      LocalDateTime createdAt,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      List<BackfillDayView> days) {
    public double progressRate() {
      if (totalDayCount == 0) return 100;
      return Math.round(
              (successDayCount + failedDayCount + skippedDayCount) * 10000.0 / totalDayCount)
          / 100.0;
    }
  }

  public record BackfillDayView(
      long dayId,
      LocalDate baseDate,
      String status,
      String skipReason,
      UUID collectionJobId,
      List<String> retryDatasets,
      int attemptCount,
      String error,
      LocalDateTime startedAt,
      LocalDateTime completedAt) {}
}
