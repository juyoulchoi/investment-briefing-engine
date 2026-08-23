package com.nanum.investment.marketdata.application;

import com.nanum.investment.marketdata.domain.KrxDataset;
import com.nanum.investment.marketdata.infrastructure.KrxCollectionJobRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KrxCollectionJobRunner {
  private final KrxMarketDataService collector;
  private final KrxCollectionJobRepository jobs;
  private final com.nanum.investment.marketdata.infrastructure.KrxRequestRateLimiter rateLimiter;

  public KrxCollectionJobRunner(
      KrxMarketDataService collector,
      KrxCollectionJobRepository jobs,
      com.nanum.investment.marketdata.infrastructure.KrxRequestRateLimiter rateLimiter) {
    this.collector = collector;
    this.jobs = jobs;
    this.rateLimiter = rateLimiter;
  }

  @Async("krxCollectorExecutor")
  public void run(UUID jobId, LocalDate baseDate) {
    runNow(jobId, baseDate, List.of(KrxDataset.values()), 0);
  }

  public KrxCollectionJobRepository.JobView runNow(
      UUID jobId, LocalDate baseDate, List<KrxDataset> datasets, long requestIntervalMillis) {
    jobs.markRunning(jobId);
    try {
      for (KrxDataset dataset : datasets) {
        LocalDateTime startedAt = LocalDateTime.now();
        try {
          rateLimiter.acquire(requestIntervalMillis);
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
    return jobs.find(jobId);
  }

  private String trim(String value) {
    if (value == null || value.length() <= 4000) return value;
    return value.substring(0, 4000);
  }
}
