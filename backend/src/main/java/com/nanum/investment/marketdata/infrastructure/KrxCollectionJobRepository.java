package com.nanum.investment.marketdata.infrastructure;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KrxCollectionJobRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public KrxCollectionJobRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void create(UUID id, LocalDate baseDate, int totalCount) {
    jdbc.update(
        """
                INSERT INTO tb_krx_clct_job(id, base_date, status, total_count)
                VALUES (:id, :baseDate, 'QUEUED', :totalCount)
                """,
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("baseDate", Date.valueOf(baseDate))
            .addValue("totalCount", totalCount));
  }

  public List<String> failedDatasets(UUID id) {
    return jdbc.query(
        "SELECT dataset_code FROM tb_krx_clct_job_item WHERE job_id=:id AND status<>'SUCCESS' ORDER BY dataset_code",
        Map.of("id", id),
        (rs, row) -> rs.getString(1));
  }

  public void markRunning(UUID id) {
    jdbc.update(
        "UPDATE tb_krx_clct_job SET status='RUNNING', started_at=CURRENT_TIMESTAMP WHERE id=:id",
        Map.of("id", id));
  }

  public void saveItem(
      UUID jobId,
      String dataset,
      String status,
      int receivedCount,
      long storedCount,
      String errorMessage,
      LocalDateTime startedAt) {
    jdbc.update(
        """
                INSERT INTO tb_krx_clct_job_item(job_id,dataset_code,status,received_count,stored_count,
                  error_message,started_at,completed_at)
                VALUES (:jobId,:dataset,:status,:receivedCount,:storedCount,:errorMessage,:startedAt,CURRENT_TIMESTAMP)
                ON CONFLICT (job_id,dataset_code) DO UPDATE SET status=EXCLUDED.status,
                  received_count=EXCLUDED.received_count,stored_count=EXCLUDED.stored_count,
                  error_message=EXCLUDED.error_message,started_at=EXCLUDED.started_at,completed_at=CURRENT_TIMESTAMP
                """,
        new MapSqlParameterSource()
            .addValue("jobId", jobId)
            .addValue("dataset", dataset)
            .addValue("status", status)
            .addValue("receivedCount", receivedCount)
            .addValue("storedCount", storedCount)
            .addValue("errorMessage", errorMessage)
            .addValue("startedAt", Timestamp.valueOf(startedAt)));
  }

  public void markCompleted(UUID id) {
    jdbc.update(
        """
                        UPDATE tb_krx_clct_job job SET
                          success_count=(SELECT count(*) FROM tb_krx_clct_job_item WHERE job_id=job.id AND status='SUCCESS'),
                          failed_count=(SELECT count(*) FROM tb_krx_clct_job_item WHERE job_id=job.id AND status<>'SUCCESS'),
                          status=CASE WHEN EXISTS (SELECT 1 FROM tb_krx_clct_job_item WHERE job_id=job.id AND status<>'SUCCESS')
                            THEN 'COMPLETED_WITH_ERRORS' ELSE 'COMPLETED' END,
                          completed_at=CURRENT_TIMESTAMP WHERE id=:id
                        """,
        Map.of("id", id));
  }

  public JobView find(UUID id) {
    JobView job =
        jdbc.queryForObject(
            "SELECT * FROM tb_krx_clct_job WHERE id=:id",
            Map.of("id", id),
            (rs, row) ->
                new JobView(
                    rs.getObject("id", UUID.class),
                    rs.getDate("base_date").toLocalDate(),
                    rs.getString("status"),
                    rs.getInt("total_count"),
                    rs.getInt("success_count"),
                    rs.getInt("failed_count"),
                    time(rs.getTimestamp("created_at")),
                    time(rs.getTimestamp("started_at")),
                    time(rs.getTimestamp("completed_at")),
                    List.of()));
    List<ItemView> items =
        jdbc.query(
            "SELECT * FROM tb_krx_clct_job_item WHERE job_id=:id ORDER BY id",
            Map.of("id", id),
            (rs, row) ->
                new ItemView(
                    rs.getString("dataset_code"),
                    rs.getString("status"),
                    rs.getInt("received_count"),
                    rs.getLong("stored_count"),
                    rs.getString("error_message"),
                    time(rs.getTimestamp("started_at")),
                    time(rs.getTimestamp("completed_at"))));
    return new JobView(
        job.jobId(),
        job.baseDate(),
        job.status(),
        job.totalCount(),
        job.successCount(),
        job.failedCount(),
        job.createdAt(),
        job.startedAt(),
        job.completedAt(),
        items);
  }

  private static LocalDateTime time(Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }

  public record JobView(
      UUID jobId,
      LocalDate baseDate,
      String status,
      int totalCount,
      int successCount,
      int failedCount,
      LocalDateTime createdAt,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      List<ItemView> items) {}

  public record ItemView(
      String dataset,
      String status,
      int receivedCount,
      long storedCount,
      String error,
      LocalDateTime startedAt,
      LocalDateTime completedAt) {}
}
