package com.nanum.investment.briefing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SectorPriceTrendFactorCalculator {
  static final String FACTOR_CODE = "PRICE_TREND";
  static final int RETURN_HORIZON = 20;
  static final int LOOKBACK_OBSERVATIONS = 252;
  private static final int MAX_STALE_CALENDAR_DAYS = 7;
  private static final int REQUIRED_SOURCE_VALUES = RETURN_HORIZON + LOOKBACK_OBSERVATIONS + 1;
  private static final List<SectorDefinition> SECTORS =
      List.of(
          new SectorDefinition("SEMICONDUCTOR", "반도체", "KRX_63e609cfb0e0e055d8367c9f76"),
          new SectorDefinition("FOOD_BEVERAGE", "음식료·담배", "KRX_c0a35d233fe5d225c006e17ddd"),
          new SectorDefinition("TEXTILE_APPAREL", "섬유·의류", "KRX_318b7cc220e3f4f2c787377d7a"),
          new SectorDefinition("PAPER_WOOD", "종이·목재", "KRX_6d3e17861c47ada222f44fd0aa"),
          new SectorDefinition("CHEMICAL", "화학", "KRX_ec38e0cc835b7e78cfdd147c0e"),
          new SectorDefinition("PHARMACEUTICAL", "제약", "KRX_584f87292f4e622ddd8b2e2af9"),
          new SectorDefinition("NON_METAL", "비금속", "KRX_97f4e7139b7e03b8aa0e20ddfd"),
          new SectorDefinition("STEEL_METAL", "금속", "KRX_dbcdcc9bdeaec39801411125c7"),
          new SectorDefinition("MACHINERY", "기계·장비", "KRX_a9f359bd699f50fcda96fcdb54"),
          new SectorDefinition("ELECTRIC_ELECTRONIC", "전기전자", "KRX_74c3fe05cacbe7a4bca6735e01"),
          new SectorDefinition("TRANSPORT_EQUIPMENT", "운송장비·부품", "KRX_c6cd23d7a913fbf62094d4374e"),
          new SectorDefinition("DISTRIBUTION", "유통", "KRX_58c9bf1fb679f2ac0f20eca723"),
          new SectorDefinition("ELECTRIC_GAS", "전기·가스", "KRX_b07d1e6a9c7ffbaae76a28c785"),
          new SectorDefinition("CONSTRUCTION", "건설", "KRX_1c63c48996495c7635d240f5c1"),
          new SectorDefinition("TRANSPORT_WAREHOUSE", "운송·창고", "KRX_cb4dbba36bf5db38b9eda11dd4"),
          new SectorDefinition("TELECOMMUNICATION", "통신", "KRX_c83f51e49e0cd39bffb5e4f497"),
          new SectorDefinition("FINANCE", "금융", "KRX_08be9886e217a232a85ba27f37"),
          new SectorDefinition("BANK", "은행", "KRX_b075f6fd99831debdb73fef3a1"),
          new SectorDefinition("SECURITIES", "증권", "KRX_e9228c3e1e1639bebcedc2a3c3"),
          new SectorDefinition("INSURANCE", "보험", "KRX_fa8e5cda2655f766b81a7da9f9"),
          new SectorDefinition("SERVICE", "일반서비스", "KRX_901f2a772de2e800a314fd83fa"),
          new SectorDefinition("MANUFACTURING", "제조", "KRX_3c6c6628256d46ce40e6b88c5e"));

  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public SectorPriceTrendFactorCalculator(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Result calculate(LocalDate baseDate) {
    List<MetricResult> metrics = SECTORS.stream().map(sector -> calculate(baseDate, sector)).toList();
    List<MetricResult> available = metrics.stream().filter(MetricResult::available).toList();
    if (available.isEmpty()) {
      return new Result("MISSING", "계산 가능한 업종 가격 추세가 없습니다.", null, 0, metrics);
    }

    BigDecimal score =
        available.stream()
            .map(MetricResult::score)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(available.size()), 4, RoundingMode.HALF_UP);
    int confidence = available.size() * 100 / SECTORS.size();
    String status = available.size() == SECTORS.size() ? "AVAILABLE" : "PARTIAL";
    String reason =
        "AVAILABLE".equals(status)
            ? null
            : "요청 업종 " + SECTORS.size() + "개 중 " + available.size() + "개만 계산 가능합니다.";
    return new Result(status, reason, score, confidence, metrics);
  }

  public void saveMetrics(long predictionId, Result result) {
    int availableCount = (int) result.metrics().stream().filter(MetricResult::available).count();
    BigDecimal metricWeight =
        availableCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(100).divide(BigDecimal.valueOf(availableCount), 4, RoundingMode.HALF_UP);
    for (MetricResult metric : result.metrics()) {
      BigDecimal effectiveWeight = metric.available() ? metricWeight : BigDecimal.ZERO;
      BigDecimal contribution =
          metric.available()
              ? metric.score().multiply(effectiveWeight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
              : null;
      jdbc.sql(
              """
              INSERT INTO "TB_MKT_DIR_PRED_METRIC"(
                "MKT_DIR_PRED_ID","FACTOR_CD","METRIC_CD","SOURCE_TABLE","SOURCE_KEY_JSON",
                "SOURCE_BASE_DT","RAW_VALUE","LOOKBACK_DAYS","SAMPLE_COUNT","ROLLING_MEDIAN",
                "ROLLING_MAD","ROBUST_Z","METRIC_SCORE","PLANNED_WEIGHT","EFFECTIVE_WEIGHT",
                "CONTRIBUTION","DATA_STATUS","FORMULA_CD","PARAM_JSON","SOURCE_DATA_HASH")
              VALUES(:predictionId,:factorCode,:metricCode,'TB_IDX_DAY',CAST(:sourceKey AS jsonb),
                :sourceDate,:rawValue,:lookback,:sampleCount,:median,:mad,:robustZ,:score,:plannedWeight,
                :effectiveWeight,:contribution,:status,'RETURN_20D_ROBUST_Z_MAD_V1',
                CAST(:parameters AS jsonb),:sourceHash)
              """)
          .param("predictionId", predictionId)
          .param("factorCode", FACTOR_CODE)
          .param("metricCode", "SECTOR_" + metric.sector().code())
          .param(
              "sourceKey",
              toJson(
                  Map.of(
                      "indexCode", metric.sector().indexCode(),
                      "sectorCode", metric.sector().code(),
                      "sectorName", metric.sector().name())))
          .param("sourceDate", metric.sourceDate())
          .param("rawValue", metric.rawReturn())
          .param("lookback", LOOKBACK_OBSERVATIONS)
          .param("sampleCount", metric.sampleCount())
          .param("median", metric.median())
          .param("mad", metric.mad())
          .param("robustZ", metric.robustZ())
          .param("score", metric.score())
          .param("plannedWeight", BigDecimal.valueOf(100).divide(BigDecimal.valueOf(SECTORS.size()), 4, RoundingMode.HALF_UP))
          .param("effectiveWeight", effectiveWeight)
          .param("contribution", contribution)
          .param("status", metric.status())
          .param(
              "parameters",
              toJson(
                  Map.of(
                      "returnHorizonObservations", RETURN_HORIZON,
                      "lookbackObservations", LOOKBACK_OBSERVATIONS,
                      "scoreDirection", "HIGHER_RETURN_IS_HIGHER_SCORE",
                      "zScoreLimit", 3.5,
                      "reason", metric.reason() == null ? "" : metric.reason())))
          .param("sourceHash", metric.sourceHash())
          .update();
    }
  }

  private MetricResult calculate(LocalDate baseDate, SectorDefinition sector) {
    List<IndexObservation> observations =
        jdbc.sql(
                """
                SELECT "TRADE_DT", "CLS_VAL"
                FROM "TB_IDX_DAY"
                WHERE "IDX_CD"=:indexCode AND "TRADE_DT"<=:baseDate
                  AND "CLS_VAL" IS NOT NULL AND "CLS_VAL">0
                  AND "DATA_STS" IN ('FRESH','PARTIAL','STALE')
                ORDER BY "TRADE_DT" DESC
                LIMIT :requiredValues
                """)
            .param("indexCode", sector.indexCode())
            .param("baseDate", baseDate)
            .param("requiredValues", REQUIRED_SOURCE_VALUES)
            .query(
                (rs, rowNum) ->
                    new IndexObservation(rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)))
            .list();
    if (observations.isEmpty()) {
      return MetricResult.unavailable(sector, "MISSING", "기준일 이하의 업종지수가 없습니다.", 0);
    }
    if (ChronoUnit.DAYS.between(observations.get(0).date(), baseDate) > MAX_STALE_CALENDAR_DAYS) {
      return MetricResult.unavailable(
          sector, "STALE", "최신 업종지수 기준일이 오래되었습니다.", observations.size());
    }
    if (observations.size() < REQUIRED_SOURCE_VALUES) {
      return MetricResult.unavailable(
          sector,
          "INSUFFICIENT_HISTORY",
          "20일 수익률 분포 계산에 " + REQUIRED_SOURCE_VALUES + "개 지수값이 필요합니다.",
          observations.size());
    }

    List<Double> returns = new ArrayList<>(LOOKBACK_OBSERVATIONS);
    for (int offset = 1; offset <= LOOKBACK_OBSERVATIONS; offset++) {
      returns.add(returnRate(observations.get(offset).value(), observations.get(offset + RETURN_HORIZON).value()));
    }
    double currentReturn = returnRate(observations.get(0).value(), observations.get(RETURN_HORIZON).value());
    RobustZScoreCalculator.Result normalized = RobustZScoreCalculator.calculate(currentReturn, returns);
    if (normalized.status() != RobustZScoreCalculator.Status.AVAILABLE) {
      return MetricResult.unavailable(sector, "INVALID", normalized.reason(), returns.size());
    }
    return new MetricResult(
        sector,
        "AVAILABLE",
        null,
        observations.get(0).date(),
        BigDecimal.valueOf(currentReturn),
        returns.size(),
        BigDecimal.valueOf(normalized.median()),
        BigDecimal.valueOf(normalized.medianAbsoluteDeviation()),
        BigDecimal.valueOf(normalized.rawZScore()),
        BigDecimal.valueOf(normalized.normalizedScore()),
        sourceHash(observations));
  }

  static double returnRate(BigDecimal current, BigDecimal previous) {
    return current.subtract(previous).divide(previous, 12, RoundingMode.HALF_UP).doubleValue() * 100;
  }

  private String sourceHash(List<IndexObservation> observations) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      observations.stream()
          .sorted(Comparator.comparing(IndexObservation::date))
          .forEach(
              row ->
                  digest.update(
                      (row.date() + "=" + row.value().toPlainString() + "\n")
                          .getBytes(StandardCharsets.UTF_8)));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
    }
  }

  private String toJson(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("업종 가격 추세 근거를 JSON으로 변환할 수 없습니다.", e);
    }
  }

  private record SectorDefinition(String code, String name, String indexCode) {}

  private record IndexObservation(LocalDate date, BigDecimal value) {}

  public record Result(
      String status, String reason, BigDecimal score, int confidence, List<MetricResult> metrics) {
    public boolean available() {
      return "AVAILABLE".equals(status) || "PARTIAL".equals(status);
    }
  }

  public record MetricResult(
      SectorDefinition sector,
      String status,
      String reason,
      LocalDate sourceDate,
      BigDecimal rawReturn,
      int sampleCount,
      BigDecimal median,
      BigDecimal mad,
      BigDecimal robustZ,
      BigDecimal score,
      String sourceHash) {
    static MetricResult unavailable(
        SectorDefinition sector, String status, String reason, int sampleCount) {
      return new MetricResult(
          sector, status, reason, null, null, sampleCount, null, null, null, null, null);
    }

    public boolean available() {
      return "AVAILABLE".equals(status);
    }
  }
}
