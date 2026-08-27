package com.nanum.investment.marketdata.application;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import com.nanum.investment.marketdata.infrastructure.KofiaClient;
import com.nanum.investment.marketdata.infrastructure.KofiaRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaRepository.JobView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KofiaCollectionService {
  private static final long MAX_RANGE_DAYS = 36525;
  private final KofiaClient client;
  private final KofiaRepository repository;
  private final KofiaCollectionJobRunner runner;

  public KofiaCollectionService(
      KofiaClient client, KofiaRepository repository, KofiaCollectionJobRunner runner) {
    this.client = client;
    this.repository = repository;
    this.runner = runner;
  }

  public List<DatasetView> datasets() {
    return List.of(KofiaDataset.values()).stream()
        .map(
            d ->
                new DatasetView(
                    d.name(), d.serviceId(), d.objectName(), d.description(), d.path(), false))
        .toList();
  }

  public CollectionView collect(KofiaDataset dataset, LocalDate from, LocalDate to, UUID jobId) {
    validatePeriod(from, to);
    KofiaClient.KofiaResponse response = client.collect(dataset, from, to);
    String hash = sha256(response.rawResponse().toString());
    int stored =
        repository.save(jobId, dataset, from, to, response.rawResponse(), response.rows(), hash);
    return new CollectionView(dataset.name(), from, to, response.rows().size(), stored, hash);
  }

  public List<Map<String, Object>> creditBalances(LocalDate from, LocalDate to, int limit) {
    validatePeriod(from, to);
    return repository.creditBalances(from, to, limit);
  }

  public JobView startJob(LocalDate from, LocalDate to, List<String> requestedDatasets) {
    validatePeriod(from, to);
    List<KofiaDataset> datasets = selectDatasets(requestedDatasets);
    UUID id = UUID.randomUUID();
    try {
      repository.createJob(id, from, to, datasets);
    } catch (IllegalStateException error) {
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, error.getMessage());
    }
    runner.run(id);
    return repository.job(id);
  }

  public JobView job(UUID id) {
    try {
      return repository.job(id);
    } catch (NoSuchElementException error) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, error.getMessage());
    }
  }

  public List<JobView> jobs(int limit) {
    return repository.jobs(limit);
  }

  public JobView retryFailures(UUID id) {
    JobView current = job(id);
    if (!List.of("FAILED", "COMPLETED_WITH_ERRORS").contains(current.status()))
      throw invalid("FAILED 또는 COMPLETED_WITH_ERRORS 상태에서만 실패 항목을 재실행할 수 있습니다.");
    if (repository.retryFailures(id) == 0) throw invalid("재실행할 실패 항목이 없습니다.");
    runner.run(id);
    return repository.job(id);
  }

  private List<KofiaDataset> selectDatasets(List<String> values) {
    if (values == null || values.isEmpty()) return List.of(KofiaDataset.values());
    try {
      return values.stream().map(KofiaDataset::fromCode).distinct().toList();
    } catch (IllegalArgumentException error) {
      throw invalid(error.getMessage());
    }
  }

  private void validatePeriod(LocalDate from, LocalDate to) {
    if (from == null || to == null) throw invalid("from과 to가 필요합니다.");
    if (from.isAfter(to)) throw invalid("from은 to보다 늦을 수 없습니다.");
    if (to.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) throw invalid("to는 현재 날짜보다 늦을 수 없습니다.");
    if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS)
      throw invalid("수집 기간은 100년을 초과할 수 없습니다.");
  }

  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }

  private BusinessException invalid(String message) {
    return new BusinessException(ErrorCode.INVALID_REQUEST, message);
  }

  public record DatasetView(
      String datasetCode,
      String serviceId,
      String objectName,
      String description,
      String path,
      boolean authenticationRequired) {}

  public record CollectionView(
      String dataset,
      LocalDate from,
      LocalDate to,
      int receivedCount,
      int storedCount,
      String responseHash) {}
}
