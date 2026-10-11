package com.nanum.investment.common.infrastructure;

import com.nanum.collector.CollectorExchangeScheduler;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** One process owns collection and V1 calculations; both migration histories remain independent. */
@Configuration
@ConditionalOnProperty(name = "unified.single-backend", havingValue = "true")
@Import(CollectorExchangeScheduler.class)
public class SingleBackendConfiguration {
  @Bean
  FlywayMigrationStrategy sourceThenModelMigrations() {
    return model -> {
      Flyway.configure()
          .dataSource(model.getConfiguration().getDataSource())
          .defaultSchema("source")
          .schemas("source")
          .table("flyway_collector_history")
          .locations("classpath:db/collector", "classpath:db/unified/source")
          .baselineOnMigrate(false)
          .load()
          .migrate();
      model.migrate();
    };
  }
}
