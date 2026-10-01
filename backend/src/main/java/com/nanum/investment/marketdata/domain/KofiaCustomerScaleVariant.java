package com.nanum.investment.marketdata.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public record KofiaCustomerScaleVariant(
    UUID variantId,
    Stage stage,
    String regionCode,
    String fundKindCode,
    String offeringTypeCode,
    String sellerCode,
    String sellerName,
    String metricTypeCode,
    String parameterHash) {
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

  public KofiaCustomerScaleVariant {
    regionCode = normalizeScope(regionCode);
    fundKindCode = normalizeScope(fundKindCode);
    sellerCode = normalizeScope(sellerCode);
  }

  public Map<String, Object> requestParameters(LocalDate baseDate) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("tmpV4", requestScope(regionCode));
    values.put("tmpV5", requestScope(fundKindCode));
    values.put("tmpV7", offeringTypeCode);
    values.put("tmpV13", requestScope(sellerCode));
    values.put("tmpV34", baseDate.format(DATE));
    values.put("tmpV38", metricTypeCode);
    values.put("tmpV40", "1");
    values.put("tmpV41", "1");
    values.put("OBJ_NM", "STATFND0100200180BO");
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
    REGION,
    FUND_KIND,
    SELLER,
    VALID_VARIANT,
    ALL_ACTIVE;

    public static Stage from(String value) {
      if (value == null || value.isBlank()) throw new IllegalArgumentException("수집 단계가 필요합니다.");
      try {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException error) {
        throw new IllegalArgumentException("지원하지 않는 고객유형별규모 수집 단계입니다: " + value);
      }
    }
  }
}
