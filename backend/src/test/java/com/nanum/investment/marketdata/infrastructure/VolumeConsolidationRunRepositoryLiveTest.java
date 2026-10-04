package com.nanum.investment.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanum.investment.marketdata.application.VolumeConsolidationRunService;
import com.nanum.investment.marketdata.application.VolumeConsolidationService;
import com.nanum.investment.marketdata.domain.VolumeConsolidation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Only a session-local temporary table is created; all changes are rolled back. */
@EnabledIfEnvironmentVariable(named = "SCREEN_DB_URL", matches = ".+")
class VolumeConsolidationRunRepositoryLiveTest {
  @Test
  void migrationAndSnapshotRoundtripOnTemporaryTable() throws Exception {
    try (var connection =
        DriverManager.getConnection(
            System.getenv("SCREEN_DB_URL"),
            System.getenv("SCREEN_DB_USER"),
            System.getenv("SCREEN_DB_PASSWORD"))) {
      connection.setAutoCommit(false);
      var jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
      String migration =
          Files.readString(
              Path.of(
                  "backend/src/main/resources/db/migration/V150__add_volume_consolidation_run.sql"));
      jdbc.sql(
              migration.replace(
                  "CREATE TABLE \"TB_VOL_CONSOL_RUN\"", "CREATE TEMP TABLE \"TB_VOL_CONSOL_RUN\""))
          .update();
      assertThat(
              jdbc.sql(
                      "SELECT relpersistence::text FROM pg_class WHERE oid='\"TB_VOL_CONSOL_RUN\"'::regclass")
                  .query(String.class)
                  .single())
          .isEqualTo("t");
      var repository = new VolumeConsolidationRunRepository(jdbc);
      var mapper = new ObjectMapper().findAndRegisterModules();
      var service =
          new VolumeConsolidationRunService(
              org.mockito.Mockito.mock(VolumeConsolidationService.class), repository, mapper);
      UUID id = UUID.randomUUID();
      LocalDate date = LocalDate.of(2025, 1, 2);
      repository.start(id, "AFTER_KRX_COLLECTION", date, "VOLUME_CONSOLIDATION_V1");
      assertThat(repository.find(id).orElseThrow().status()).isEqualTo("RUNNING");
      Screen screen =
          new Screen(
              date,
              date,
              date,
              date,
              date,
              75,
              "DATE_GAPS_UNVERIFIED",
              List.of(date),
              Rules.defaults(),
              List.of(),
              0,
              0,
              Map.of(),
              List.of("미확인"),
              List.of());
      repository.finish(id, "PARTIAL", date, 0, mapper.writeValueAsString(screen), null);
      assertThat(repository.find(id).orElseThrow().hasResult()).isTrue();
      assertThat(service.result(id)).isEqualTo(screen);
      assertThat(repository.latest(10)).hasSize(1);
      UUID interrupted = UUID.randomUUID();
      repository.start(interrupted, "MANUAL", null, "VOLUME_CONSOLIDATION_V1");
      assertThat(repository.recoverInterrupted(OffsetDateTime.now().plusSeconds(1))).isEqualTo(1);
      assertThat(repository.find(interrupted).orElseThrow().status()).isEqualTo("FAILED");
      connection.rollback();
    }
  }
}
