package com.nanum.investment.marketdata.application;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.marketdata.domain.KofiaCustomerScaleVariant;
import com.nanum.investment.marketdata.domain.KofiaCustomerScaleVariant.Stage;
import com.nanum.investment.marketdata.infrastructure.KofiaClient;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository.CodeValue;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository.JobView;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository.PendingItem;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository.SaveResult;
import com.nanum.investment.marketdata.infrastructure.KofiaCustomerScaleRepository.VariantSeed;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KofiaCustomerScaleService {
  public static final LocalDate DEFAULT_BACKFILL_FROM = LocalDate.of(2024, 1, 2);
  private static final long MAX_RANGE_DAYS = 36525;
  private static final List<String> OFFERING_TYPES = List.of("1", "2");
  private static final List<String> METRIC_TYPES = List.of("1", "2");
  private static final List<CodeValue> REGIONS =
      List.of(
          new CodeValue("1", "국내"),
          new CodeValue("4", "해외"),
          new CodeValue("3", "해외30"),
          new CodeValue("2", "해외60"));

  private final KofiaClient client;
  private final KofiaCustomerScaleRepository repository;
  private final KofiaCustomerScaleJobRunner runner;

  public KofiaCustomerScaleService(
      KofiaClient client,
      KofiaCustomerScaleRepository repository,
      KofiaCustomerScaleJobRunner runner) {
    this.client = client;
    this.repository = repository;
    this.runner = runner;
  }

  @Transactional
  public VariantSyncView syncVariants() {
    List<CodeValue> fundKinds = repository.activeCodes("T1100");
    List<CodeValue> sellers = repository.activeSellers();
    if (fundKinds.size() != 8)
      throw new IllegalStateException("펀드종류 T1100 활성 코드가 8개가 아닙니다: " + fundKinds.size());
    if (sellers.isEmpty()) throw new IllegalStateException("수집할 활성 판매회사가 없습니다.");

    List<VariantSeed> seeds = new ArrayList<>();
    addSeeds(seeds, Stage.AGGREGATE, "*", "*", "*", null);
    for (CodeValue region : REGIONS) addSeeds(seeds, Stage.REGION, region.code(), "*", "*", null);
    for (CodeValue fundKind : fundKinds)
      addSeeds(seeds, Stage.FUND_KIND, "*", fundKind.code(), "*", null);
    for (CodeValue seller : sellers)
      addSeeds(seeds, Stage.SELLER, "*", "*", seller.code(), seller.name());

    repository.syncGeneratedVariants(seeds);
    return new VariantSyncView(
        seeds.size(),
        OFFERING_TYPES.size() * METRIC_TYPES.size(),
        REGIONS.size() * OFFERING_TYPES.size() * METRIC_TYPES.size(),
        fundKinds.size() * OFFERING_TYPES.size() * METRIC_TYPES.size(),
        sellers.size() * OFFERING_TYPES.size() * METRIC_TYPES.size());
  }

  @Transactional
  public KofiaCustomerScaleVariant registerValidVariant(
      String regionCode,
      String fundKindCode,
      String offeringTypeCode,
      String sellerCode,
      String metricTypeCode) {
    validateOffering(offeringTypeCode);
    validateMetric(metricTypeCode);
    if (REGIONS.stream().noneMatch(value -> value.code().equals(regionCode)))
      throw invalid("투자지역 코드가 아닙니다: " + regionCode);
    String normalizedKind = validateCode("T1100", fundKindCode, "펀드종류");
    CodeValue seller =
        repository.activeSellers().stream()
            .filter(value -> value.code().equals(sellerCode))
            .findFirst()
            .orElseThrow(() -> invalid("활성 판매회사 코드가 아닙니다: " + sellerCode));
    VariantSeed seed =
        seed(
            Stage.VALID_VARIANT,
            regionCode,
            normalizedKind,
            offeringTypeCode,
            seller.code(),
            seller.name(),
            metricTypeCode);
    repository.upsertVariants(List.of(seed));
    return repository.variants(Stage.VALID_VARIANT).stream()
        .filter(value -> value.parameterHash().equals(seed.parameterHash()))
        .findFirst()
        .orElseThrow();
  }

  public JobView startJob(Stage stage, LocalDate from, LocalDate to) {
    validatePeriod(from, to);
    syncVariants();
    UUID jobId = UUID.randomUUID();
    try {
      repository.createJob(jobId, stage, from, to);
    } catch (IllegalStateException error) {
      throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, error.getMessage());
    }
    runner.run(jobId);
    return repository.job(jobId, false);
  }

  public SaveResult collect(PendingItem item) {
    KofiaClient.KofiaResponse response =
        client.collectCustomerScale(item.variant(), item.baseDate());
    return repository.save(item, response);
  }

  public JobView retryFailures(UUID jobId) {
    JobView current = job(jobId, false);
    if (!List.of("FAILED", "COMPLETED_WITH_ERRORS").contains(current.status()))
      throw invalid("FAILED 또는 COMPLETED_WITH_ERRORS 상태에서만 재실행할 수 있습니다.");
    if (repository.retryFailures(jobId) == 0) throw invalid("재실행할 실패 항목이 없습니다.");
    runner.run(jobId);
    return repository.job(jobId, false);
  }

  public JobView job(UUID jobId, boolean includeItems) {
    try {
      return repository.job(jobId, includeItems);
    } catch (NoSuchElementException error) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, error.getMessage());
    }
  }

  public List<JobView> jobs(int limit) {
    return repository.jobs(limit);
  }

  public List<Map<String, Object>> variants(Stage stage) {
    return repository.variantViews(stage);
  }

  public List<Map<String, Object>> rows(LocalDate from, LocalDate to, Stage stage, int limit) {
    validatePeriod(from, to);
    return repository.rows(from, to, stage, limit);
  }

  private void addSeeds(
      List<VariantSeed> seeds,
      Stage stage,
      String region,
      String fundKind,
      String seller,
      String sellerName) {
    for (String offering : OFFERING_TYPES)
      for (String metric : METRIC_TYPES)
        seeds.add(seed(stage, region, fundKind, offering, seller, sellerName, metric));
  }

  private VariantSeed seed(
      Stage stage,
      String region,
      String fundKind,
      String offering,
      String seller,
      String sellerName,
      String metric) {
    String canonical = String.join("|", region, fundKind, offering, seller, metric);
    return new VariantSeed(
        stage, region, fundKind, offering, seller, sellerName, metric, sha256(canonical));
  }

  private String validateCode(String group, String code, String name) {
    if (code == null || code.isBlank()) throw invalid(name + " 코드가 필요합니다.");
    return repository.activeCodes(group).stream()
        .map(CodeValue::code)
        .filter(code::equals)
        .findFirst()
        .orElseThrow(() -> invalid("활성 " + name + " 코드가 아닙니다: " + code));
  }

  private void validateOffering(String value) {
    if (!OFFERING_TYPES.contains(value)) throw invalid("공모/사모 코드는 1 또는 2여야 합니다.");
  }

  private void validateMetric(String value) {
    if (!METRIC_TYPES.contains(value)) throw invalid("판매잔고/계좌수 코드는 1 또는 2여야 합니다.");
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

  public record VariantSyncView(
      int totalCount, int aggregateCount, int regionCount, int fundKindCount, int sellerCount) {}
}
