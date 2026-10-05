package com.nanum.collector;

import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CollectorStatusController {
  private final JdbcClient jdbc;

  public CollectorStatusController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/api/v1/collector/status")
  public Map<String, Object> status() {
    return Map.of(
        "service",
        "investment-kofia-collector",
        "database",
        jdbc.sql("SELECT current_database()").query(String.class).single(),
        "schemaVersion",
        jdbc.sql("SELECT max(version) FROM flyway_collector_history WHERE success")
            .query(String.class)
            .single());
  }
}
