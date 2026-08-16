package com.nanum.investment.scheduler;

import com.nanum.investment.domain.*;
import com.nanum.investment.repository.TbSchLogRepository;
import java.time.*;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulerExecutionService {
  private final JdbcClient jdbc;
  private final TbSchLogRepository logs;

  public SchedulerExecutionService(JdbcClient jdbc, TbSchLogRepository logs) {
    this.jdbc = jdbc;
    this.logs = logs;
  }

  @Transactional
  public <T> ExecutionResult<T> execute(
      String jobCode, String jobName, LocalDate baseDate, Supplier<T> task) {
    String traceId = UUID.randomUUID().toString();
    OffsetDateTime started = OffsetDateTime.now();
    boolean acquired =
        Boolean.TRUE.equals(
            jdbc.sql("SELECT pg_try_advisory_xact_lock(hashtext(:key))")
                .param("key", "SCHEDULER:" + jobCode)
                .query(Boolean.class)
                .single());
    if (!acquired) return new ExecutionResult<>(false, true, traceId, null, "동일 작업이 이미 실행 중입니다.");
    TbSchLog log =
        logs.save(
            TbSchLog.builder()
                .traceId(traceId)
                .jobCode(jobCode)
                .jobName(jobName)
                .baseDate(baseDate)
                .startDateTime(started)
                .jobStatus(SchedulerJobStatus.RUNNING)
                .build());
    try {
      T value = task.get();
      finish(log, SchedulerJobStatus.SUCCESS, started, 1, 0, null);
      return new ExecutionResult<>(true, false, traceId, value, null);
    } catch (RuntimeException ex) {
      finish(log, SchedulerJobStatus.FAILED, started, 0, 1, limit(ex.getMessage(), 2000));
      throw ex;
    }
  }

  private void finish(
      TbSchLog log,
      SchedulerJobStatus status,
      OffsetDateTime started,
      int success,
      int failure,
      String error) {
    OffsetDateTime ended = OffsetDateTime.now();
    log.setEndDateTime(ended);
    log.setJobStatus(status);
    log.setSuccessCount(success);
    log.setFailureCount(failure);
    log.setElapsedMilliseconds(Duration.between(started, ended).toMillis());
    log.setErrorMessage(error);
    logs.save(log);
  }

  private String limit(String value, int max) {
    return value == null ? null : value.substring(0, Math.min(value.length(), max));
  }

  public record ExecutionResult<T>(
      boolean success, boolean skipped, String traceId, T data, String message) {}
}
