package com.nanum.investment.marketdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KrxCollectionJobRunner {
  private final KrxMarketDataService collector;
  private final KrxCollectionJobRepository jobs;

  public KrxCollectionJobRunner(KrxMarketDataService collector, KrxCollectionJobRepository jobs) {
    this.collector = collector;
    this.jobs = jobs;
  }

  @Async("krxCollectorExecutor")
  public void run(UUID jobId, LocalDate baseDate) {
    jobs.markRunning(jobId);
    try {
      for (KrxDataset dataset : KrxDataset.values()) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
          var result = collector.collect(dataset, baseDate);
          jobs.saveItem(
              jobId,
              dataset.name(),
              "SUCCESS",
              result.receivedCount(),
              result.storedCount(),
              null,
              startedAt);
        } catch (RestClientResponseException exception) {
          jobs.saveItem(
              jobId,
              dataset.name(),
              "HTTP_" + exception.getStatusCode().value(),
              0,
              0,
              trim(exception.getResponseBodyAsString()),
              startedAt);
        } catch (RuntimeException exception) {
          jobs.saveItem(
              jobId, dataset.name(), "FAILED", 0, 0, trim(exception.getMessage()), startedAt);
        }
      }
    } finally {
      jobs.markCompleted(jobId);
    }
  }

  private String trim(String value) {
    if (value == null || value.length() <= 4000) return value;
    return value.substring(0, 4000);
  }
}
