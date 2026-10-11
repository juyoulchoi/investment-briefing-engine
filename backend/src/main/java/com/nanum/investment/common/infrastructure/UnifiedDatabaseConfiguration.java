package com.nanum.investment.common.infrastructure;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Opt-in schema routing validation and isolated rehearsal startup. */
@Configuration
@ConditionalOnProperty(name = "unified.database.enabled", havingValue = "true")
public class UnifiedDatabaseConfiguration {
  @Bean
  @ConditionalOnProperty(name = "unified.database.candidate", havingValue = "true")
  static BeanFactoryPostProcessor disableCandidateBackgroundWork() {
    return factory -> {
      BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
      for (String name : registry.getBeanDefinitionNames()) {
        String type = registry.getBeanDefinition(name).getBeanClassName();
        if (type != null && type.startsWith("com.nanum.investment.")
            && (type.endsWith("Scheduler") || type.endsWith("Recovery"))) {
          registry.removeBeanDefinition(name);
        }
      }
    };
  }

  @Bean
  ApplicationRunner validateUnifiedDatabase(JdbcClient jdbc, Environment environment) {
    return args -> {
      String model = environment.getRequiredProperty("unified.database.model");
      if (!model.equals("v1") && !model.equals("v2")) {
        throw new IllegalStateException("Unified model must be v1 or v2");
      }
      String current = jdbc.sql("SELECT current_schema()").query(String.class).single();
      if (!model.equals(current)) {
        throw new IllegalStateException("JDBC currentSchema must start with " + model);
      }
      String relation = jdbc.sql("""
          SELECT n.nspname || '.' || c.relname || ':' || c.relkind::text
          FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
          WHERE c.oid=to_regclass('"TB_HOLD"')
          """).query(String.class).single();
      if (!relation.equals(model + ".TB_HOLD:v")) {
        throw new IllegalStateException("TB_HOLD must resolve to the model portfolio view");
      }
      Boolean shared = jdbc.sql("""
          SELECT to_regclass('"TB_ACCT"')=to_regclass('portfolio."TB_ACCT"')
             AND to_regclass('"TB_PRC_DAY"')=to_regclass('source."TB_PRC_DAY"')
          """).query(Boolean.class).single();
      if (!Boolean.TRUE.equals(shared)) {
        throw new IllegalStateException("Account and market source routes are not shared");
      }
      if (environment.getProperty("unified.database.candidate", Boolean.class, false)) {
        String database = jdbc.sql("SELECT current_database()").query(String.class).single();
        if (!database.startsWith("investment_unified_")) {
          throw new IllegalStateException("Candidate startup requires an isolated unified database");
        }
      }
    };
  }
}
