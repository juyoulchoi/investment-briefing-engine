package com.nanum.investment.briefing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class InvestorFlowExcelImportDatabaseTest {
  @Test
  @EnabledIfEnvironmentVariable(named = "RUN_INVESTOR_FLOW_DB_IMPORT", matches = "true")
  void importsRequestedFilesIntoDatabase() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            System.getenv().getOrDefault(
                "INVESTOR_FLOW_DB_URL", "jdbc:postgresql://postgres:5432/investment"),
            System.getenv().getOrDefault("INVESTOR_FLOW_DB_USERNAME", "investment"),
            System.getenv().getOrDefault("INVESTOR_FLOW_DB_PASSWORD", "investment"));
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    InvestorFlowExcelImportService service =
        new InvestorFlowExcelImportService(
            JdbcClient.create(dataSource),
            jdbcTemplate,
            new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
            System.getenv().getOrDefault("INVESTOR_FLOW_IMPORT_DIR", "/data/investor-flow"));

    InvestorFlowExcelImportService.ImportResult result = service.importByFilenameToken("20240101");

    assertThat(result.matchedFiles()).isEqualTo(90);
    assertThat(result.completedFiles()).isEqualTo(89);
    assertThat(result.failedFiles()).isEqualTo(1);
    assertThat(result.insertedRawRows()).isEqualTo(517_836);
    assertThat(result.normalizedRows()).isEqualTo(87_264);
  }
}
