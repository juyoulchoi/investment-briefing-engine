package com.nanum.investment.marketdata.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public record KofiaCompanyFundFlowVariant(
    UUID variantId,
    Stage stage,
    String fundTypeCode,
    String fundKindCode,
    String offeringTypeCode,
    String etfIncludeYn,
    String parameterHash) {
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

  public KofiaCompanyFundFlowVariant {
    fundTypeCode = normalizeScope(fundTypeCode);
    fundKindCode = normalizeScope(fundKindCode);
    offeringTypeCode = normalizeScope(offeringTypeCode);
  }

  public Map<String, Object> requestParameters(LocalDate baseDate) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("tmpV3", requestScope(fundTypeCode));
    values.put("tmpV5", requestScope(fundKindCode));
    values.put("tmpV7", requestScope(offeringTypeCode));
    values.put("tmpV19", etfIncludeYn);
    values.put("tmpV34", baseDate.format(DATE));
    values.put("tmpV40", "1");
    values.put("tmpV41", "1");
    values.put("OBJ_NM", "STATFND0200100040BO");
    return Map.copyOf(values);
  }

  private static String normalizeScope(String value) {
    return value == null || value.isBlank() ? "*" : value.trim();
  }

  private static String requestScope(String value) {
    return "*".equals(value) ? "" : value;
  }

  public enum Stage {
    AGGREGATE,
    OFFERING,
    FUND_TYPE,
    FUND_KIND,
    VALID_VARIANT,
    ALL_ACTIVE;

    public static Stage from(String value) {
      if (value == null || value.isBlank()) throw new IllegalArgumentException("수집 단계가 필요합니다.");
      try {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException error) {
        throw new IllegalArgumentException("지원하지 않는 회사별자금유출입 수집 단계입니다: " + value);
      }
    }
  }
}
