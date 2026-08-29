package com.nanum.investment.common.infrastructure.external;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerSupport {
  private final JdbcClient jdbc;
  private final Map<String, MemoryState> memory = new ConcurrentHashMap<>();

  CircuitBreakerSupport() { this.jdbc = null; }

  @Autowired
  public CircuitBreakerSupport(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public <T> T execute(
      String key, int failureThreshold, Duration openDuration, Supplier<T> action) {
    acquire(key, openDuration);
    try {
      T value = action.get();
      reset(key);
      return value;
    } catch (RuntimeException ex) {
      recordFailure(key, failureThreshold);
      throw ex;
    }
  }

  private void acquire(String key, Duration openDuration) {
    if (jdbc == null) {
      MemoryState value = memory.computeIfAbsent(key, ignored -> new MemoryState());
      synchronized (value) {
        if ("CLOSED".equals(value.state)) return;
        if ("HALF_OPEN".equals(value.state)) throw new CircuitOpenException(key);
        if (value.openedAt != null && Instant.now().isBefore(value.openedAt.plus(openDuration)))
          throw new CircuitOpenException(key);
        value.state = "HALF_OPEN";
      }
      return;
    }
    jdbc.sql("INSERT INTO \"TB_EXT_CIRCUIT\"(\"CIRCUIT_KEY\") VALUES(:key) ON CONFLICT(\"CIRCUIT_KEY\") DO NOTHING")
        .param("key", key).update();
    Map<String, Object> row = jdbc.sql("SELECT \"STATE\",\"OPENED_AT\" FROM \"TB_EXT_CIRCUIT\" WHERE \"CIRCUIT_KEY\"=:key")
        .param("key", key).query().singleRow();
    String state = row.get("STATE").toString();
    if ("CLOSED".equals(state)) return;
    if ("HALF_OPEN".equals(state)) throw new CircuitOpenException(key);
    OffsetDateTime openedAt = (OffsetDateTime) row.get("OPENED_AT");
    if (openedAt != null && OffsetDateTime.now().isBefore(openedAt.plus(openDuration)))
      throw new CircuitOpenException(key);
    int acquired = jdbc.sql("UPDATE \"TB_EXT_CIRCUIT\" SET \"STATE\"='HALF_OPEN',\"HALF_OPEN_IN_FLIGHT\"=TRUE,\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"CIRCUIT_KEY\"=:key AND \"STATE\"='OPEN' AND \"HALF_OPEN_IN_FLIGHT\"=FALSE")
        .param("key", key).update();
    if (acquired == 0) throw new CircuitOpenException(key);
  }

  private void recordFailure(String key, int threshold) {
    if (jdbc == null) {
      MemoryState value = memory.get(key);
      synchronized (value) {
        value.failures = "HALF_OPEN".equals(value.state) ? threshold : value.failures + 1;
        if (value.failures >= threshold) {
          value.state = "OPEN";
          value.openedAt = Instant.now();
        }
      }
      return;
    }
    jdbc.sql("""
        UPDATE "TB_EXT_CIRCUIT" SET
          "FAILURE_CNT"=CASE WHEN "STATE"='HALF_OPEN' THEN :threshold ELSE "FAILURE_CNT"+1 END,
          "STATE"=CASE WHEN "STATE"='HALF_OPEN' OR "FAILURE_CNT"+1>=:threshold THEN 'OPEN' ELSE 'CLOSED' END,
          "OPENED_AT"=CASE WHEN "STATE"='HALF_OPEN' OR "FAILURE_CNT"+1>=:threshold THEN CURRENT_TIMESTAMP ELSE "OPENED_AT" END,
          "HALF_OPEN_IN_FLIGHT"=FALSE,"UPD_DTTM"=CURRENT_TIMESTAMP WHERE "CIRCUIT_KEY"=:key
        """).param("key", key).param("threshold", threshold).update();
  }

  public void reset(String key) {
    if (jdbc == null) {
      memory.remove(key);
      return;
    }
    jdbc.sql("UPDATE \"TB_EXT_CIRCUIT\" SET \"STATE\"='CLOSED',\"FAILURE_CNT\"=0,\"OPENED_AT\"=NULL,\"HALF_OPEN_IN_FLIGHT\"=FALSE,\"UPD_DTTM\"=CURRENT_TIMESTAMP WHERE \"CIRCUIT_KEY\"=:key")
        .param("key", key).update();
  }

  public List<Map<String, Object>> states() {
    if (jdbc == null) return List.of();
    return jdbc.sql("SELECT \"CIRCUIT_KEY\" circuit_key,\"STATE\" state,\"FAILURE_CNT\" failure_count,\"OPENED_AT\" opened_at,\"HALF_OPEN_IN_FLIGHT\" half_open_in_flight,\"UPD_DTTM\" updated_at FROM \"TB_EXT_CIRCUIT\" ORDER BY \"CIRCUIT_KEY\"")
        .query().listOfRows();
  }

  public boolean isOpen(String key, Duration ignored) {
    if (jdbc == null) {
      MemoryState value = memory.get(key);
      return value != null && "OPEN".equals(value.state);
    }
    return jdbc.sql("SELECT EXISTS(SELECT 1 FROM \"TB_EXT_CIRCUIT\" WHERE \"CIRCUIT_KEY\"=:key AND \"STATE\"='OPEN')")
        .param("key", key).query(Boolean.class).single();
  }

  private static final class MemoryState {
    String state = "CLOSED";
    int failures;
    Instant openedAt;
  }

  public static class CircuitOpenException extends RuntimeException {
    public CircuitOpenException(String key) {
      super("Circuit breaker is open: " + key);
    }
  }
}
