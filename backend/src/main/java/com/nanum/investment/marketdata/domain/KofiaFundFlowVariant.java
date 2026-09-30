package com.nanum.investment.marketdata.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public record KofiaFundFlowVariant(
    UUID variantId,
    Stage stage,
    String fundTypeCode,
    String fundKindCode,
    String offeringTypeCode,
    String managerCode,
    String managerName,
    String etfIncludeYn,
    String parameterHash) {
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

  public KofiaFundFlowVariant {
    fundTypeCode = normalizeScope(fundTypeCode);
    fundKindCode = normalizeScope(fundKindCode);
    managerCode = normalizeScope(managerCode);
  }

  public Map<String, Object> requestParameters(LocalDate from, LocalDate to) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("tmpV3", requestScope(fundTypeCode));
    values.put("tmpV5", requestScope(fundKindCode));
    values.put("tmpV7", offeringTypeCode);
    values.put("tmpV11", requestScope(managerCode));
    values.put("tmpV19", etfIncludeYn);
    values.put("tmpV30", from.format(DATE));
    values.put("tmpV31", to.format(DATE));
    values.put("tmpV37", "0");
    values.put("tmpV40", "1");
    values.put("tmpV41", "1");
    values.put("OBJ_NM", "STATFND0100100030BO");
    return Map.copyOf(values);
  }

  public String canonicalParameters() {
    return String.join(
        "|", fundTypeCode, fundKindCode, offeringTypeCode, managerCode, etfIncludeYn);
  }

  private static String normalizeScope(String value) {
    return value == null || value.isBlank() ? "*" : value.trim();
  }

  private static String requestScope(String value) {
    return "*".equals(value) ? "" : value;
  }

  public enum Stage {
    AGGREGATE,
    FUND_TYPE,
    FUND_KIND,
    MANAGER,
    VALID_VARIANT,
    ALL_ACTIVE;

    public static Stage from(String value) {
      if (value == null || value.isBlank()) throw new IllegalArgumentException("수집 단계가 필요합니다.");
      try {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException error) {
        throw new IllegalArgumentException("지원하지 않는 펀드 자금유출입 수집 단계입니다: " + value);
      }
    }
  }
}
