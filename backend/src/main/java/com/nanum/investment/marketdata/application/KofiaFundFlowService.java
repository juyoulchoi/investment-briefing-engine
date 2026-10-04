package com.nanum.investment.marketdata.application;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.marketdata.domain.KofiaFundFlowVariant;
import com.nanum.investment.marketdata.domain.KofiaFundFlowVariant.Stage;
import com.nanum.investment.marketdata.infrastructure.KofiaClient;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.CodeValue;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.JobView;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.PendingItem;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.SaveResult;
import com.nanum.investment.marketdata.infrastructure.KofiaFundFlowRepository.VariantSeed;
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
public class KofiaFundFlowService {
  public static final LocalDate DEFAULT_BACKFILL_FROM = LocalDate.of(2024, 1, 2);
  private static final long MAX_RANGE_DAYS = 36525;
  private static final List<String> OFFERING_TYPES = List.of("1", "2");
  private static final List<String> ETF_OPTIONS = List.of("Y", "N");

  private final KofiaClient client;
  private final KofiaFundFlowRepository repository;
  private final KofiaFundFlowJobRunner runner;

  public KofiaFundFlowService(
      KofiaClient client, KofiaFundFlowRepository repository, KofiaFundFlowJobRunner runner) {
    this.client = client;
    this.repository = repository;
    this.runner = runner;
  }

  @Transactional
  public VariantSyncView syncVariants() {
    List<CodeValue> fundTypes = repository.activeCodes("T1111");
    List<CodeValue> fundKinds = repository.activeCodes("T1100");
    List<CodeValue> managers = repository.activeManagers();
    if (fundTypes.size() != 14)
      throw new IllegalStateException("펀드유형 T1111 활성 코드가 14개가 아닙니다: " + fundTypes.size());
    if (fundKinds.size() != 8)
      throw new IllegalStateException("펀드종류 T1100 활성 코드가 8개가 아닙니다: " + fundKinds.size());
    if (managers.isEmpty()) throw new IllegalStateException("수집할 활성 운용회사가 없습니다.");

    List<VariantSeed> seeds = new ArrayList<>();
    for (String offering : OFFERING_TYPES)
      for (String etf : ETF_OPTIONS)
        seeds.add(seed(Stage.AGGREGATE, "*", "*", offering, "*", null, etf));
    for (CodeValue fundType : fundTypes)
      for (String offering : OFFERING_TYPES)
        for (String etf : ETF_OPTIONS)
          seeds.add(seed(Stage.FUND_TYPE, fundType.code(), "*", offering, "*", null, etf));
    for (CodeValue fundKind : fundKinds)
      for (String offering : OFFERING_TYPES)
        for (String etf : ETF_OPTIONS)
          seeds.add(seed(Stage.FUND_KIND, "*", fundKind.code(), offering, "*", null, etf));
    for (CodeValue manager : managers)
      for (String offering : OFFERING_TYPES)
        for (String etf : ETF_OPTIONS)
          seeds.add(seed(Stage.MANAGER, "*", "*", offering, manager.code(), manager.name(), etf));

    repository.syncGeneratedVariants(seeds);
    return new VariantSyncView(
        seeds.size(),
        OFFERING_TYPES.size() * ETF_OPTIONS.size(),
        fundTypes.size() * OFFERING_TYPES.size() * ETF_OPTIONS.size(),
        fundKinds.size() * OFFERING_TYPES.size() * ETF_OPTIONS.size(),
        managers.size() * OFFERING_TYPES.size() * ETF_OPTIONS.size());
  }

  @Transactional
  public KofiaFundFlowVariant registerValidVariant(
      String fundType,
      String fundKind,
      String offeringType,
      String managerCode,
      String etfIncludeYn) {
    validateOffering(offeringType);
    validateEtf(etfIncludeYn);
    String normalizedType = validateCode("T1111", fundType, "펀드유형");
    String normalizedKind = validateCode("T1100", fundKind, "펀드종류");
    CodeValue manager =
        repository.activeManagers().stream()
            .filter(value -> value.code().equals(managerCode))
            .findFirst()
            .orElseThrow(() -> invalid("활성 운용회사 코드가 아닙니다: " + managerCode));
    VariantSeed seed =
        seed(
            Stage.VALID_VARIANT,
            normalizedType,
            normalizedKind,
            offeringType,
            manager.code(),
            manager.name(),
            etfIncludeYn);
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
        client.collectFundFlow(item.variant(), item.from(), item.to());
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

  private VariantSeed seed(
      Stage stage,
      String fundType,
      String fundKind,
      String offering,
      String manager,
      String managerName,
      String etf) {
    String canonical = String.join("|", fundType, fundKind, offering, manager, etf);
    return new VariantSeed(
        stage, fundType, fundKind, offering, manager, managerName, etf, sha256(canonical));
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

  private void validateEtf(String value) {
    if (!List.of("Y", "N").contains(value)) throw invalid("ETF 포함여부는 Y 또는 N이어야 합니다.");
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
      int totalCount, int aggregateCount, int fundTypeCount, int fundKindCount, int managerCount) {}
}
