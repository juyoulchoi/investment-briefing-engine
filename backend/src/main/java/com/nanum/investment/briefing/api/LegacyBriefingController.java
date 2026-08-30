package com.nanum.investment.briefing.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.briefing.application.HistoricalMarketRecalculationService;
import com.nanum.investment.briefing.application.LegacyBriefingImportService;
import com.nanum.investment.briefing.application.MarketOutcomeEvaluationService;
import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legacy-briefings")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "LEGACY 브리핑",
    description = "과거 ChatGPT 판단 원본·추출·역사 재계산 비교 API")
public class LegacyBriefingController {
  private final LegacyBriefingImportService importer;
  private final HistoricalMarketRecalculationService recalculations;
  private final JdbcClient jdbc;
  private final MarketOutcomeEvaluationService outcomes;

  public LegacyBriefingController(
      LegacyBriefingImportService importer,
      HistoricalMarketRecalculationService recalculations,
      MarketOutcomeEvaluationService outcomes,
      JdbcClient jdbc) {
    this.importer = importer;
    this.recalculations = recalculations;
    this.outcomes = outcomes;
    this.jdbc = jdbc;
  }

  @PostMapping("/imports/workspace-recovery-files")
  @io.swagger.v3.oas.annotations.Operation(summary = "워크스페이스 복원 JSON의 LEGACY 원본 및 자동추출 이관")
  public ApiResponse<LegacyBriefingImportService.ImportResult> importWorkspaceFiles(
      HttpServletRequest request) {
    return ApiResponse.success(importer.importWorkspaceFiles(), TraceIdUtils.resolve(request));
  }

  @PostMapping("/imports")
  @io.swagger.v3.oas.annotations.Operation(summary = "요청 JSON의 LEGACY 원본 및 자동추출 이관")
  public ApiResponse<LegacyBriefingImportService.ImportResult> importDocuments(
      @RequestBody ImportRequest input, HttpServletRequest request) {
    return ApiResponse.success(
        importer.importDocuments(input.index(), input.recovered()), TraceIdUtils.resolve(request));
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(summary = "LEGACY 브리핑 및 구조화 추출 이력 조회")
  public ApiResponse<List<LegacyRow>> history(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      HttpServletRequest request) {
    LocalDate start = from == null ? LocalDate.of(1900, 1, 1) : from;
    LocalDate end = to == null ? LocalDate.of(2999, 12, 31) : to;
    List<LegacyRow> rows =
        jdbc.sql(
                """
                SELECT l."LEGACY_BRF_ID",l."GENERATED_DTTM",l."MARKET_BASE_DT",l."RECOVERY_DT",l."BRF_TP",
                       l."RECOVERY_STS",l."ISSUE_TXT",l."CANDIDATE_CNT",l."MESSAGE_ID",l."ORIGINAL_HASH",
                       e."MKT_RISK_SCR",e."MKT_PHASE_CD",e."REG_BUY_SIG_CD",e."ADD_BUY_SIG_CD",e."CASH_INPUT_RT",
                       e."EXTRACT_CONF_RT",e."REVIEW_STS"
                  FROM "TB_LEGACY_BRF" l
                  LEFT JOIN "TB_LEGACY_BRF_EXTRACT" e ON e."LEGACY_BRF_ID"=l."LEGACY_BRF_ID" AND e."EXTRACT_VER"=1
                 WHERE l."RECOVERY_DT" BETWEEN :from AND :to
                 ORDER BY l."RECOVERY_DT"
                """)
            .param("from", start)
            .param("to", end)
            .query(
                (rs, n) ->
                    new LegacyRow(
                        rs.getLong(1),
                        rs.getObject(2, OffsetDateTime.class),
                        rs.getObject(3, LocalDate.class),
                        rs.getObject(4, LocalDate.class),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getInt(8),
                        rs.getString(9),
                        rs.getString(10),
                        (Integer) rs.getObject(11),
                        rs.getString(12),
                        rs.getString(13),
                        rs.getString(14),
                        rs.getBigDecimal(15),
                        rs.getBigDecimal(16),
                        rs.getString(17)))
            .list();
    return ApiResponse.success(rows, TraceIdUtils.resolve(request));
  }

  @PostMapping("/historical-recalculations")
  @io.swagger.v3.oas.annotations.Operation(summary = "운영 최신값을 변경하지 않는 시장 공통 과거 재계산")
  public ApiResponse<HistoricalMarketRecalculationService.RecalculationResult> recalculate(
      @Valid @RequestBody RecalculationRequest input, HttpServletRequest request) {
    return ApiResponse.success(
        recalculations.recalculate(input.baseDate(), input.ruleVersion(), input.codeVersion()),
        TraceIdUtils.resolve(request));
  }

  @GetMapping("/comparisons")
  @io.swagger.v3.oas.annotations.Operation(summary = "LEGACY 판단과 최신 역사 재계산 결과 비교")
  public ApiResponse<List<HistoricalMarketRecalculationService.ComparisonRow>> comparisons(
      @RequestParam LocalDate from, @RequestParam LocalDate to, HttpServletRequest request) {
    return ApiResponse.success(recalculations.comparisons(from, to), TraceIdUtils.resolve(request));
  }

  @PostMapping("/outcomes")
  @io.swagger.v3.oas.annotations.Operation(summary = "KOSPI·KOSDAQ·S&P500 D+1/5/20/60 시장성과 계산")
  public ApiResponse<MarketOutcomeEvaluationService.BatchResult> outcomes(
      @Valid @RequestBody OutcomeRequest input, HttpServletRequest request) {
    return ApiResponse.success(
        outcomes.evaluate(input.from(), input.to()), TraceIdUtils.resolve(request));
  }

  public record RecalculationRequest(
      @NotNull LocalDate baseDate, String ruleVersion, String codeVersion) {}

  public record ImportRequest(JsonNode index, JsonNode recovered) {}

  public record OutcomeRequest(@NotNull LocalDate from, @NotNull LocalDate to) {}

  public record LegacyRow(
      Long legacyBriefingId,
      OffsetDateTime generatedAt,
      LocalDate marketBaseDate,
      LocalDate recoveryDate,
      String briefingType,
      String recoveryStatus,
      String issue,
      int candidateCount,
      String messageId,
      String originalHash,
      Integer marketRiskScore,
      String marketPhase,
      String regularBuySignal,
      String additionalBuySignal,
      BigDecimal cashInputRate,
      BigDecimal extractionConfidenceRate,
      String reviewStatus) {}
}
