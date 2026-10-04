package com.nanum.investment.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nanum.investment.marketdata.api.VolumeConsolidationController;
import com.nanum.investment.marketdata.infrastructure.VolumeConsolidationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Opt-in live verification: one explicitly read-only transaction, no application/schedulers/Flyway.
 */
@EnabledIfEnvironmentVariable(named = "SCREEN_DB_URL", matches = ".+")
class VolumeConsolidationLiveTest {
  @Test
  void readsLiveCoverageAndScreenThroughControllerWithoutWritingDatabase() throws Exception {
    try (var connection =
        DriverManager.getConnection(
            System.getenv("SCREEN_DB_URL"),
            System.getenv("SCREEN_DB_USER"),
            System.getenv("SCREEN_DB_PASSWORD"))) {
      connection.setReadOnly(true);
      connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
      connection.setAutoCommit(false);
      var repository =
          new VolumeConsolidationRepository(
              JdbcClient.create(new SingleConnectionDataSource(connection, true)));
      var service = new VolumeConsolidationService(repository);
      var mapper =
          new ObjectMapper()
              .findAndRegisterModules()
              .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      var mvc =
          MockMvcBuilders.standaloneSetup(new VolumeConsolidationController(service))
              .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
              .build();
      var response =
          mvc.perform(get("/api/v1/krx/screens/volume-consolidation"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      var json = new ObjectMapper().readTree(response);
      assertThat(json.path("availableDays").asInt()).isEqualTo(75);
      assertThat(json.path("universeCount").asInt()).isGreaterThan(0);
      assertThat(json.path("baseDate").asText()).isNotBlank();
      for (var row : json.path("rows")) {
        if (row.path("quantitativeMatch").asBoolean()) {
          assertThat(row.path("exclusionReasons").isEmpty()).isTrue();
          assertThat(row.path("verificationStatus").asText()).isEqualTo("REQUIRES_VERIFICATION");
          if (json.path("unverifiedWeekdays").isEmpty()) {
            var tracking =
                service.track(
                    row.path("market").asText(),
                    row.path("stockCode").asText(),
                    LocalDate.parse(row.path("baseDate").asText()),
                    LocalDate.parse(row.path("baseDate").asText()));
            assertThat(tracking.state()).isEqualTo("WATCHING");
            assertThat(tracking.events()).isEmpty();
          }
        }
      }
      Files.createDirectories(Path.of("build/reports/volume-consolidation"));
      Files.writeString(Path.of("build/reports/volume-consolidation/live-screen.json"), response);
      connection.rollback();
    }
  }
}
