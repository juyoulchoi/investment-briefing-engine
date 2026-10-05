package com.nanum.collector;

import com.nanum.investment.common.domain.TbApiLog;
import com.nanum.investment.common.infrastructure.repository.TbApiLogRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Separate process and database; does not scan investment models, accounts or briefing jobs. */
@Configuration
@EnableAutoConfiguration
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(
    basePackageClasses = TbApiLogRepository.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*\\.(?!TbApiLogRepository)[^.]*"))
@ComponentScan(
    basePackages = {
      "com.nanum.collector",
      "com.nanum.investment.marketdata",
      "com.nanum.investment.common.infrastructure.external"
    },
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = {
              "^(?!com\\.nanum\\.investment\\.marketdata\\..*\\.Kofia|com\\.nanum\\.investment\\.common\\.infrastructure\\.external\\.|com\\.nanum\\.collector\\.Collector).*",
              ".*KofiaConsumer.*"
            }))
public class KofiaCollectorApplication {
  @Bean
  PersistenceManagedTypes collectorEntities() {
    return PersistenceManagedTypes.of(TbApiLog.class.getName());
  }

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(KofiaCollectorApplication.class);
    app.setAdditionalProfiles("kofia-collector");
    app.run(args);
  }
}
