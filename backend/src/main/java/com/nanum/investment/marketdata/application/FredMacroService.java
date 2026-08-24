package com.nanum.investment.marketdata.application;

import com.nanum.investment.common.exception.*;
import com.nanum.investment.marketdata.infrastructure.*;
import com.nanum.investment.marketdata.infrastructure.FredMacroRepository.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FredMacroService {
  private static final long MAX_RANGE_DAYS = 36525;
  private static final Map<String, BondProjection> BONDS =
      Map.of(
          "DGS2", new BondProjection("미국 국채 2년", 24),
          "DGS10", new BondProjection("미국 국채 10년", 120),
          "DGS30", new BondProjection("미국 국채 30년", 360),
          "DFII10", new BondProjection("미국 물가연동국채 실질금리 10년", 120));

  private final FredClient client;
  private final FredMacroRepository repository;
  private final FredMacroJobRunner runner;
  private final JdbcClient jdbc;

  public FredMacroService(
      FredClient client,
      FredMacroRepository repository,
      FredMacroJobRunner runner,
      JdbcClient jdbc) {
    this.client = client;
    this.repository = repository;
    this.runner = runner;
    this.jdbc = jdbc;
  }

  public SeriesView register(String requestedCode, SeriesCommand command) {
    String code = normalize(requestedCode);
    validateCommand(command);
    return repository.upsertSeries(client.metadata(code), command);
  }

  public SeriesView refreshMetadata(String requestedCode) {
    SeriesView current = findSeries(requestedCode);
    SeriesCommand command =
        new SeriesCommand(
            current.seriesName(),
            current.categoryCode(),
            current.countryCode(),
            current.transformCode(),
            current.aggregationCode(),
            current.collectionCycleCode(),
            current.refreshOverlapDays(),
            current.vintagePolicyCode(),
            current.targetCode(),
            current.active() ? "Y" : "N");
    return repository.upsertSeries(client.metadata(current.seriesCode()), command);
  }

  public List<SeriesView> series(boolean activeOnly) {
    return repository.series(activeOnly);
  }

  public SeriesView findSeries(String code) {
    try {
      return repository.series(normalize(code));
    } catch (NoSuchElementException error) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, error.getMessage());
    }
  }

  public SeriesView setActive(String code, boolean active) {
    String normalized = normalize(code);
    try {
      repository.setActive(normalized, active);
      return repository.series(normalized);
    } catch (NoSuchElementException error) {
      throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, error.getMessage());
    }
  }

  public List<Map<String, Object>> observations(String code, LocalDate from, LocalDate to) {
    validatePeriod(from, to);
    findSeries(code);
    return repository.observations(normalize(code), from, to);
  }

  public List<Map<String, Object>> revisions(String code, LocalDate from, LocalDate to) {
    validatePeriod(from, to);
    findSeries(code);
    return repository.revisions(normalize(code), from, to);
  }

  public synchronized JobView startJob(
      LocalDate from, LocalDate to, List<String> requestedCodes, long interval) {
    validatePeriod(from, to);
    if (interval < 0 || interval > 60000) throw invalid("requestIntervalMillis는 0~60000 범위여야 합니다.");
    List<SeriesView> selected = selectSeries(requestedCodes);
    String codes =
        selected.stream()
            .map(SeriesView::seriesCode)
            .sorted()
            .reduce((a, b) -> a + "," + b)
            .orElseThrow();
    if (repository.hasActiveOverlap(from, to, codes))
      throw new BusinessException(
          ErrorCode.DUPLICATE_RESOURCE, "동일 Series의 기간이 겹치는 활성 FRED 수집 Job이 있습니다.");
    UUID id = UUID.randomUUID();
    repository.createJob(id, from, to, selected, interval);
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

  public JobView pause(UUID id) {
    job(id);
    state(() -> repository.requestPause(id));
    return job(id);
  }

  public JobView resume(UUID id) {
    job(id);
    state(() -> repository.resume(id));
    runner.run(id);
    return job(id);
  }

  public JobView cancel(UUID id) {
    job(id);
    state(() -> repository.requestCancel(id));
    return job(id);
  }

  public JobView retryFailures(UUID id) {
    JobView job = job(id);
    if (!List.of("COMPLETED_WITH_ERRORS", "FAILED").contains(job.status()))
      throw invalid("COMPLETED_WITH_ERRORS 또는 FAILED 상태에서만 실패 Series를 재처리할 수 있습니다.");
    if (repository.retryFailures(id) == 0) throw invalid("재처리할 실패 Series가 없습니다.");
    runner.run(id);
    return job(id);
  }

  @Transactional
  public MacroCollectionResult collectItem(PendingItem item, UUID jobId) {
    List<FredClient.Observation> values =
        client.observations(
            item.seriesCode(),
            item.from(),
            item.to(),
            item.transformCode(),
            item.aggregationCode());
    int inserted = 0, updated = 0, unchanged = 0, missing = 0, revisions = 0;
    LocalDate latest = null;
    for (FredClient.Observation observation : values) {
      SaveResult result =
          repository.saveObservation(item.seriesId(), jobId, observation, hash(observation));
      switch (result) {
        case INSERTED -> inserted++;
        case UPDATED -> {
          updated++;
          revisions++;
        }
        case UNCHANGED -> unchanged++;
        case MISSING_INSERTED -> {
          inserted++;
          missing++;
        }
        case MISSING_UPDATED -> {
          updated++;
          missing++;
          revisions++;
        }
        case MISSING_UNCHANGED -> {
          unchanged++;
          missing++;
        }
      }
      if (observation.value() != null) {
        latest =
            latest == null || observation.observationDate().isAfter(latest)
                ? observation.observationDate()
                : latest;
        projectBond(item.seriesCode(), observation);
      }
    }
    if (latest != null) repository.updateSeriesSuccess(item.seriesId(), latest);
    return new MacroCollectionResult(
        item.seriesCode(),
        item.from(),
        item.to(),
        values.size(),
        inserted,
        updated,
        unchanged,
        missing,
        revisions,
        latest);
  }

  private void projectBond(String code, FredClient.Observation value) {
    BondProjection bond = BONDS.get(code);
    if (bond == null || value.value() == null) return;
    BigDecimal previous =
        jdbc.sql(
                "SELECT \"YLD_RT\" FROM \"TB_BOND_DAY\" WHERE \"BOND_CD\"=:code AND \"BASE_DT\"<:day ORDER BY \"BASE_DT\" DESC LIMIT 1")
            .param("code", code)
            .param("day", value.observationDate())
            .query(BigDecimal.class)
            .optional()
            .orElse(null);
    BigDecimal bp =
        previous == null ? null : value.value().subtract(previous).multiply(new BigDecimal("100"));
    jdbc.sql(
            """
        INSERT INTO "TB_BOND_DAY"("BASE_DT","BOND_CD","BOND_NM","CNTRY_CD","MATURITY_MON","YLD_RT","PREV_YLD_RT","CHG_BP","DATA_SRC_CD","DATA_STS")
        VALUES(:day,:code,:name,'US',:months,:rate,:previous,:bp,'FRED','FRESH')
        ON CONFLICT("BASE_DT","BOND_CD") DO UPDATE SET "BOND_NM"=EXCLUDED."BOND_NM","YLD_RT"=EXCLUDED."YLD_RT",
          "PREV_YLD_RT"=EXCLUDED."PREV_YLD_RT","CHG_BP"=EXCLUDED."CHG_BP","DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP
        """)
        .param("day", value.observationDate())
        .param("code", code)
        .param("name", bond.name())
        .param("months", bond.months())
        .param("rate", value.value())
        .param("previous", previous)
        .param("bp", bp)
        .update();
  }

  private List<SeriesView> selectSeries(List<String> requested) {
    if (requested == null || requested.isEmpty()) {
      List<SeriesView> active = repository.series(true);
      if (active.isEmpty()) throw invalid("수집할 활성 FRED Series가 없습니다.");
      return active;
    }
    return requested.stream().map(this::findSeries).filter(SeriesView::active).distinct().toList();
  }

  private void validatePeriod(LocalDate from, LocalDate to) {
    if (from == null || to == null) throw invalid("from과 to가 필요합니다.");
    if (from.isAfter(to)) throw invalid("from은 to보다 늦을 수 없습니다.");
    if (to.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) throw invalid("to는 현재 날짜보다 늦을 수 없습니다.");
    if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS)
      throw invalid("한 번의 FRED 수집 기간은 100년을 초과할 수 없습니다.");
  }

  private void validateCommand(SeriesCommand command) {
    if (command == null) throw invalid("Series 등록 정보가 필요합니다.");
    if (command.refreshOverlapDays() != null
        && (command.refreshOverlapDays() < 0 || command.refreshOverlapDays() > 3650))
      throw invalid("refreshOverlapDays는 0~3650 범위여야 합니다.");
    if (command.vintagePolicyCode() != null
        && !Set.of("LATEST_ONLY", "REVISION_HISTORY").contains(command.vintagePolicyCode()))
      throw invalid("지원하지 않는 Vintage 정책입니다.");
  }

  private void state(Runnable action) {
    try {
      action.run();
    } catch (IllegalStateException error) {
      throw invalid(error.getMessage());
    }
  }

  private String normalize(String code) {
    if (code == null || code.isBlank()) throw invalid("FRED Series 코드가 필요합니다.");
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private String hash(FredClient.Observation value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(
                  (value.observationDate()
                          + "|"
                          + value.value()
                          + "|"
                          + value.realtimeStart()
                          + "|"
                          + value.realtimeEnd())
                      .getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }

  private BusinessException invalid(String message) {
    return new BusinessException(ErrorCode.INVALID_REQUEST, message);
  }

  private record BondProjection(String name, int months) {}
}
