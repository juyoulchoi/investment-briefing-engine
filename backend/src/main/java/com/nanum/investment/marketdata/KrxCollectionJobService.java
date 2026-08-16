package com.nanum.investment.marketdata;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KrxCollectionJobService {
  private final KrxCollectionJobRepository jobs;
  private final KrxCollectionJobRunner runner;

  public KrxCollectionJobService(KrxCollectionJobRepository jobs, KrxCollectionJobRunner runner) {
    this.jobs = jobs;
    this.runner = runner;
  }

  public KrxCollectionJobRepository.JobView start(LocalDate baseDate) {
    UUID id = UUID.randomUUID();
    jobs.create(id, baseDate, KrxDataset.values().length);
    runner.run(id, baseDate);
    return jobs.find(id);
  }

  public KrxCollectionJobRepository.JobView find(UUID jobId) {
    return jobs.find(jobId);
  }
}
