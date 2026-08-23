package com.nanum.investment.marketdata.api;

import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import com.nanum.investment.marketdata.domain.DataStatus;
import com.nanum.investment.marketdata.domain.SentimentPhase;
import com.nanum.investment.marketdata.domain.TbMktSent;
import com.nanum.investment.marketdata.domain.TbMktSnap;
import com.nanum.investment.marketdata.infrastructure.repository.TbIdxRepository;
import com.nanum.investment.marketdata.infrastructure.repository.TbMktSentRepository;
import com.nanum.investment.marketdata.infrastructure.repository.TbMktSnapRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/market-analysis")
@Transactional(readOnly = true)
@io.swagger.v3.oas.annotations.tags.Tag(name = "시장 분석", description = "시장 스냅샷 및 심리 분석 API")
public class MarketAnalysisAdminController {
  private final TbMktSnapRepository snapshots;
  private final TbMktSentRepository sentiments;
  private final TbIdxRepository indices;

  public MarketAnalysisAdminController(
      TbMktSnapRepository snapshots, TbMktSentRepository sentiments, TbIdxRepository indices) {
    this.snapshots = snapshots;
    this.sentiments = sentiments;
    this.indices = indices;
  }

  public record SnapshotRequest(
      @NotNull LocalDate baseDate,
      @NotBlank @Size(max = 30) String marketSnapshotCode,
      @NotBlank @Size(max = 100) String marketName,
      Long mainIndexId,
      BigDecimal mainIndexValue,
      BigDecimal mainIndexChangeRate,
      BigDecimal foreignNetAmount,
      BigDecimal exchangeRate,
      @Min(0) Integer advancingStockCount,
      @Min(0) Integer decliningStockCount,
      BigDecimal marketBreadthRate,
      @NotBlank @Size(max = 30) String dataSourceCode,
      @NotNull DataStatus dataStatus,
      @Min(0) Integer dataAgeMinutes) {}

  public record SnapshotRow(
      Long marketSnapshotId,
      LocalDate baseDate,
      String marketSnapshotCode,
      String marketName,
      Long mainIndexId,
      String mainIndexName,
      BigDecimal mainIndexValue,
      BigDecimal mainIndexChangeRate,
      BigDecimal foreignNetAmount,
      BigDecimal exchangeRate,
      Integer advancingStockCount,
      Integer decliningStockCount,
      BigDecimal marketBreadthRate,
      String dataSourceCode,
      DataStatus dataStatus,
      Integer dataAgeMinutes) {}

  public record SentimentRequest(
      @NotNull LocalDate baseDate,
      @NotBlank @Size(max = 30) String marketSnapshotCode,
      BigDecimal newsFearScore,
      BigDecimal aiFatigueScore,
      BigDecimal earningsConfidenceScore,
      @NotNull BigDecimal sentimentScore,
      @NotNull SentimentPhase sentimentPhase,
      @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal confidenceRate,
      @Pattern(regexp = "[YN]") String structuralDamageYn,
      @NotNull @Min(1) Integer ruleVersionNumber,
      @NotNull DataStatus dataStatus) {}

  public record SentimentRow(
      Long marketSentimentId,
      LocalDate baseDate,
      String marketSnapshotCode,
      BigDecimal newsFearScore,
      BigDecimal aiFatigueScore,
      BigDecimal earningsConfidenceScore,
      BigDecimal sentimentScore,
      SentimentPhase sentimentPhase,
      BigDecimal confidenceRate,
      String structuralDamageYn,
      Integer ruleVersionNumber,
      DataStatus dataStatus) {}

  @GetMapping("/snapshots")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 스냅샷 운영데이터 조회")
  public ApiResponse<List<SnapshotRow>> snapshots(HttpServletRequest r) {
    return ok(
        snapshots
            .findAll(Sort.by(Sort.Order.desc("baseDate"), Sort.Order.asc("marketSnapshotCode")))
            .stream()
            .map(this::row)
            .toList(),
        r);
  }

  @PostMapping("/snapshots")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 스냅샷 운영데이터 생성")
  @Transactional
  public ApiResponse<SnapshotRow> createSnapshot(
      @Valid @RequestBody SnapshotRequest b, HttpServletRequest r) {
    if (snapshots
        .findByBaseDateAndMarketSnapshotCode(b.baseDate(), b.marketSnapshotCode())
        .isPresent()) throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
    TbMktSnap x = new TbMktSnap();
    apply(x, b);
    return ok(row(snapshots.save(x)), r);
  }

  @PutMapping("/snapshots/{id}")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 스냅샷 운영데이터 수정")
  @Transactional
  public ApiResponse<SnapshotRow> updateSnapshot(
      @PathVariable Long id, @Valid @RequestBody SnapshotRequest b, HttpServletRequest r) {
    TbMktSnap x =
        snapshots
            .findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    snapshots
        .findByBaseDateAndMarketSnapshotCode(b.baseDate(), b.marketSnapshotCode())
        .filter(v -> !v.getMarketSnapshotId().equals(id))
        .ifPresent(
            v -> {
              throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
            });
    apply(x, b);
    return ok(row(snapshots.save(x)), r);
  }

  @GetMapping("/sentiments")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 심리 운영데이터 조회")
  public ApiResponse<List<SentimentRow>> sentiments(HttpServletRequest r) {
    return ok(
        sentiments
            .findAll(Sort.by(Sort.Order.desc("baseDate"), Sort.Order.asc("marketSnapshotCode")))
            .stream()
            .map(this::row)
            .toList(),
        r);
  }

  @PostMapping("/sentiments")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 심리 운영데이터 생성")
  @Transactional
  public ApiResponse<SentimentRow> createSentiment(
      @Valid @RequestBody SentimentRequest b, HttpServletRequest r) {
    if (sentiments
        .findByBaseDateAndMarketSnapshotCode(b.baseDate(), b.marketSnapshotCode())
        .isPresent()) throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
    TbMktSent x = new TbMktSent();
    apply(x, b);
    return ok(row(sentiments.save(x)), r);
  }

  @PutMapping("/sentiments/{id}")
  @io.swagger.v3.oas.annotations.Operation(summary = "시장 심리 운영데이터 수정")
  @Transactional
  public ApiResponse<SentimentRow> updateSentiment(
      @PathVariable Long id, @Valid @RequestBody SentimentRequest b, HttpServletRequest r) {
    TbMktSent x =
        sentiments
            .findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    sentiments
        .findByBaseDateAndMarketSnapshotCode(b.baseDate(), b.marketSnapshotCode())
        .filter(v -> !v.getMarketSentimentId().equals(id))
        .ifPresent(
            v -> {
              throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
            });
    apply(x, b);
    return ok(row(sentiments.save(x)), r);
  }

  private void apply(TbMktSnap x, SnapshotRequest b) {
    x.setBaseDate(b.baseDate());
    x.setMarketSnapshotCode(b.marketSnapshotCode());
    x.setMarketName(b.marketName());
    x.setMainIndex(
        b.mainIndexId() == null
            ? null
            : indices
                .findById(b.mainIndexId())
                .filter(v -> "N".equals(v.getDeleteYn()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)));
    x.setMainIndexValue(b.mainIndexValue());
    x.setMainIndexChangeRate(b.mainIndexChangeRate());
    x.setForeignNetAmount(b.foreignNetAmount());
    x.setExchangeRate(b.exchangeRate());
    x.setAdvancingStockCount(b.advancingStockCount());
    x.setDecliningStockCount(b.decliningStockCount());
    x.setMarketBreadthRate(b.marketBreadthRate());
    x.setDataSourceCode(b.dataSourceCode());
    x.setDataStatus(b.dataStatus());
    x.setDataAgeMinutes(b.dataAgeMinutes());
  }

  private void apply(TbMktSent x, SentimentRequest b) {
    x.setBaseDate(b.baseDate());
    x.setMarketSnapshotCode(b.marketSnapshotCode());
    x.setNewsFearScore(b.newsFearScore());
    x.setAiFatigueScore(b.aiFatigueScore());
    x.setEarningsConfidenceScore(b.earningsConfidenceScore());
    x.setSentimentScore(b.sentimentScore());
    x.setSentimentPhase(b.sentimentPhase());
    x.setConfidenceRate(b.confidenceRate());
    x.setStructuralDamageYn(b.structuralDamageYn() == null ? "N" : b.structuralDamageYn());
    x.setRuleVersionNumber(b.ruleVersionNumber());
    x.setDataStatus(b.dataStatus());
  }

  private SnapshotRow row(TbMktSnap x) {
    return new SnapshotRow(
        x.getMarketSnapshotId(),
        x.getBaseDate(),
        x.getMarketSnapshotCode(),
        x.getMarketName(),
        x.getMainIndex() == null ? null : x.getMainIndex().getIndexId(),
        x.getMainIndex() == null ? null : x.getMainIndex().getIndexName(),
        x.getMainIndexValue(),
        x.getMainIndexChangeRate(),
        x.getForeignNetAmount(),
        x.getExchangeRate(),
        x.getAdvancingStockCount(),
        x.getDecliningStockCount(),
        x.getMarketBreadthRate(),
        x.getDataSourceCode(),
        x.getDataStatus(),
        x.getDataAgeMinutes());
  }

  private SentimentRow row(TbMktSent x) {
    return new SentimentRow(
        x.getMarketSentimentId(),
        x.getBaseDate(),
        x.getMarketSnapshotCode(),
        x.getNewsFearScore(),
        x.getAiFatigueScore(),
        x.getEarningsConfidenceScore(),
        x.getSentimentScore(),
        x.getSentimentPhase(),
        x.getConfidenceRate(),
        x.getStructuralDamageYn(),
        x.getRuleVersionNumber(),
        x.getDataStatus());
  }

  private <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
    return ApiResponse.success(d, TraceIdUtils.resolve(r));
  }
}
