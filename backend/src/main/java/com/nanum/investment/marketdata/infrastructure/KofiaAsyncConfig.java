package com.nanum.investment.marketdata.infrastructure;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class KofiaAsyncConfig {
  @Bean("kofiaCollectorExecutor")
  public Executor kofiaCollectorExecutor(
      @Value("${kofia.collection.executor-pool-size:2}") int configuredPoolSize) {
    int poolSize = Math.min(Math.max(configuredPoolSize, 1), 4);
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(poolSize);
    executor.setMaxPoolSize(poolSize);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("kofia-collector-");
    executor.initialize();
    return executor;
  }
}
