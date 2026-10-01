package com.nanum.investment.marketdata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record KofiaOtcBondInvestorTrade(
    String dataPeriodCode,
    LocalDate from,
    LocalDate to,
    String valueTypeCode,
    String tradeTypeName,
    String bondTypeName,
    String investorTypeCode,
    String investorTypeName,
    int remainingFromMonths,
    int remainingToMonths,
    BigDecimal unitMultiplier,
    BigDecimal value) {

  private static final Map<String, InvestorType> INVESTOR_TYPES = investorTypes();

  public static List<KofiaOtcBondInvestorTrade> from(
      JsonNode row, LocalDate from, LocalDate to, Map<String, Object> requestParameters) {
    List<KofiaOtcBondInvestorTrade> values = new ArrayList<>();
    for (Map.Entry<String, InvestorType> entry : INVESTOR_TYPES.entrySet()) {
      String field = entry.getKey();
      if (!row.has(field)) continue;
      InvestorType investor = entry.getValue();
      values.add(
          new KofiaOtcBondInvestorTrade(
              text(requestParameters, "tmpV1"),
              from,
              to,
              text(requestParameters, "tmpV67"),
              row.path("TMPV1").asText(),
              row.path("TMPV2").asText(),
              investor.code(),
              investor.name(),
              remainingMonths(requestParameters, "tmpV76", "tmpV92"),
              remainingMonths(requestParameters, "tmpV77", "tmpV93"),
              decimal(requestParameters.get("tmpV40")),
              decimal(row.get(field))));
    }
    return List.copyOf(values);
  }

  private static int remainingMonths(
      Map<String, Object> parameters, String yearField, String monthField) {
    return integer(parameters.get(yearField)) * 12 + integer(parameters.get(monthField));
  }

  private static int integer(Object value) {
    if (value == null || value.toString().isBlank()) return 0;
    return Integer.parseInt(value.toString());
  }

  private static String text(Map<String, Object> parameters, String field) {
    Object value = parameters.get(field);
    return value == null ? "" : value.toString();
  }

  private static BigDecimal decimal(Object value) {
    if (value == null || value.toString().isBlank() || "null".equals(value.toString())) return null;
    return new BigDecimal(value.toString().replace(",", ""));
  }

  private static Map<String, InvestorType> investorTypes() {
    Map<String, InvestorType> values = new LinkedHashMap<>();
    values.put("TMPV3", new InvestorType("ALL", "전체"));
    values.put("TMPV4", new InvestorType("INTERDEALER", "증권사간매매"));
    values.put("G001", new InvestorType("CUSTOMER", "대고객매매"));
    values.put("TMPV5", new InvestorType("BANK", "은행"));
    values.put("TMPV6", new InvestorType("PUBLIC_ASSET_MANAGER", "자산운용(공모)"));
    values.put("TMPV7", new InvestorType("PRIVATE_ASSET_MANAGER", "자산운용(사모)"));
    values.put("TMPV8", new InvestorType("INSURANCE", "보험"));
    values.put("TMPV9", new InvestorType("MERCHANT_MUTUAL", "종금/상호"));
    values.put("TMPV10", new InvestorType("FUND_ASSOCIATION", "기금공제"));
    values.put("TMPV11", new InvestorType("FOREIGNER", "외국인"));
    values.put("TMPV12", new InvestorType("GOVERNMENT", "국가/지자체"));
    values.put("TMPV13", new InvestorType("OTHER_CORPORATION", "기타법인"));
    values.put("TMPV14", new InvestorType("INDIVIDUAL", "개인"));
    return Collections.unmodifiableMap(values);
  }

  private record InvestorType(String code, String name) {}
}
