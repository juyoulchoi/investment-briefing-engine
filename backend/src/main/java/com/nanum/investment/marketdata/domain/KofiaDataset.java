package com.nanum.investment.marketdata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum KofiaDataset {
  FINAL_QUOTED_YIELD(
      "STATBND0100000010",
      "STATBND0100000010BO",
      "채권 > 최종호가수익률",
      CollectionMode.AS_OF_RANGE_DATE,
      false,
      List.of("TMPV1", "TMPV2"),
      Map.of()),
  OTC_INVESTOR_TRADING(
      "STATBND0100000270",
      "STATBND0100000270BO",
      "채권 > 투자자별거래현황 장외",
      CollectionMode.PERIOD_ROWS,
      false,
      List.of("TMPV1", "TMPV2"),
      Map.of("tmpV67", "1", "tmpV76", "0", "tmpV77", "0", "tmpV92", "0", "tmpV93", "0")),
  FUND_FLOW_PERIOD(
      "STATFND0100100030",
      "STATFND0100100030BO",
      "펀드 > 기간자금유출입",
      CollectionMode.MONTH_RANGE,
      false,
      List.of(),
      Map.of(
          "tmpV37",
          "0",
          "tmpV5",
          "5",
          "tmpV7",
          "1",
          "tmpV3",
          "02",
          "tmpV11",
          "",
          "tmpV19",
          "Y",
          "tmpV40",
          "100000000",
          "tmpV41",
          "1")),
  CUSTOMER_TYPE_FUND_SCALE_PERIOD(
      "STATFND0100200181",
      "STATFND0100200181BO",
      "펀드 > 기간고객유형별규모",
      CollectionMode.MONTH_RANGE,
      false,
      List.of(),
      Map.of(
          "tmpV38", "1", "tmpV35", "8", "tmpV4", "*", "tmpV5", "*", "tmpV7", "1", "tmpV3", "*",
          "tmpV13", "*")),
  FUND_COMPANY_SCALE(
      "STATFND0200100010",
      "STATFND0200100010BO",
      "펀드 > 회사별설정규모",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV98", "TMPV1"),
      Map.of("tmpV6", "2", "tmpV4", "*", "tmpV5", "*", "tmpV7", "1", "tmpV20", "Y")),
  ASSET_MANAGER_FUND_FLOW(
      "STATFND0200100040",
      "STATFND0200100040BO",
      "펀드 > 운용사별자금유출입현황",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV98", "TMPV1"),
      Map.of("tmpV3", "*", "tmpV5", "*", "tmpV7", "1", "tmpV19", "Y")),
  REAL_ESTATE_FUND_COMPANY_SCALE(
      "STATFND0200100130",
      "STATFND0200100130BO",
      "부동산펀드 > 회사별설정규모",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV98", "TMPV1"),
      Map.of("tmpV100", "2", "tmpV102", "*", "tmpV5", "*", "tmpV20", "Y")),
  PRIVATE_EQUITY_COMPANY_SCALE(
      "STATFND0200100140",
      "STATFND0200100140BO",
      "사모펀드 > 회사별설정규모",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV98", "TMPV1"),
      Map.of("tmpV101", "2", "tmpV102", "*")),
  COMPANY_CUSTOMER_SALES_SCALE(
      "STATFND0200200020",
      "STATFND0200200020BO",
      "펀드 > 회사별고객유형별판매규모",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV98", "TMPV1"),
      Map.of("tmpV38", "1", "tmpV4", "*", "tmpV5", "*", "tmpV7", "1", "tmpV18", "2", "tmpV3", "*")),
  INSTALLMENT_LUMP_SUM_SCALE(
      "STATFND0200200030",
      "STATFND0200200030BO",
      "펀드 > 적립거치식규모",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV98", "TMPV1"),
      Map.of("tmpV38", "1", "tmpV4", "*", "tmpV5", "*", "tmpV7", "1", "tmpV18", "2", "tmpV3", "*")),
  KOSPI_MARKET(
      "STATSCU0100000020",
      "STATSCU0100000020BO",
      "주식 > 유가증권시장",
      CollectionMode.DATE_RANGE,
      false,
      List.of(),
      Map.of()),
  KOSDAQ_MARKET(
      "STATSCU0100000030",
      "STATSCU0100000030BO",
      "주식 > 코스닥시장",
      CollectionMode.DATE_RANGE,
      false,
      List.of(),
      Map.of()),
  MARKET_FUNDS_TREND(
      "STATSCU0100000060",
      "STATSCU0100000060BO",
      "주식 > 증시자금추이",
      CollectionMode.DATE_RANGE,
      true,
      List.of(),
      Map.of()),
  CREDIT_BALANCE_TREND(
      "STATSCU0100000070",
      "STATSCU0100000070BO",
      "주식 > 신용공여 잔고 추이",
      CollectionMode.DATE_RANGE,
      true,
      List.of(),
      Map.of()),
  CMA_DAILY_STATUS(
      "STATSCU0100000090",
      "STATSCU0100000090BO",
      "주식 > 일자별 CMA현황",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV1"),
      Map.of()),
  CMA_BALANCE_TREND(
      "STATSCU0100000110",
      "STATSCU0100000110BO",
      "주식 > 운용대상별 CMA잔고 추이",
      CollectionMode.DATE_RANGE,
      false,
      List.of(),
      Map.of("tmpV59", "", "tmpV40", "1", "tmpV41", "1")),
  SECURITIES_LENDING_DETAILS(
      "STATSCU0100000130",
      "STATSCU0100000130BO",
      "주식 > 대차거래내역",
      CollectionMode.AS_OF_DATE,
      false,
      List.of("TMPV2", "TMPV1"),
      Map.of("tmpV74", "1,0,,1")),
  SECURITIES_LENDING_TREND(
      "STATSCU0100000140",
      "STATSCU0100000140BO",
      "주식 > 대차거래추이",
      CollectionMode.DATE_RANGE,
      true,
      List.of("TMPV2"),
      Map.of("tmpV72", ""));

  public enum CollectionMode {
    DATE_RANGE,
    MONTH_RANGE,
    PERIOD_ROWS,
    AS_OF_DATE,
    AS_OF_RANGE_DATE
  }

  private final String serviceId;
  private final String objectName;
  private final String description;
  private final CollectionMode collectionMode;
  private final boolean normalized;
  private final List<String> identityFields;
  private final Map<String, String> defaultParameters;

  KofiaDataset(
      String serviceId,
      String objectName,
      String description,
      CollectionMode collectionMode,
      boolean normalized,
      List<String> identityFields,
      Map<String, String> defaultParameters) {
    this.serviceId = serviceId;
    this.objectName = objectName;
    this.description = description;
    this.collectionMode = collectionMode;
    this.normalized = normalized;
    this.identityFields = identityFields;
    this.defaultParameters = defaultParameters;
  }

  public String serviceId() {
    return serviceId;
  }

  public String objectName() {
    return objectName;
  }

  public String description() {
    return description;
  }

  public String path() {
    return "/meta/getMetaDataList.do";
  }

  public CollectionMode collectionMode() {
    return collectionMode;
  }

  public boolean normalized() {
    return normalized;
  }

  public boolean requiresSingleDateRequest() {
    return collectionMode == CollectionMode.AS_OF_DATE
        || collectionMode == CollectionMode.AS_OF_RANGE_DATE;
  }

  public Map<String, Object> requestParameters(LocalDate from, LocalDate to) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("tmpV40", "1000000");
    values.put("tmpV41", "1");
    values.putAll(defaultParameters);
    String fromValue = from.format(DateTimeFormatter.BASIC_ISO_DATE);
    String toValue = to.format(DateTimeFormatter.BASIC_ISO_DATE);
    switch (collectionMode) {
      case DATE_RANGE, PERIOD_ROWS, AS_OF_RANGE_DATE -> {
        values.put("tmpV1", "D");
        values.put("tmpV45", fromValue);
        values.put("tmpV46", toValue);
      }
      case MONTH_RANGE -> {
        values.put("tmpV30", fromValue);
        values.put("tmpV31", toValue);
      }
      case AS_OF_DATE -> values.put("tmpV34", toValue);
    }
    values.put("OBJ_NM", objectName);
    return values;
  }

  public String rowKey(JsonNode row, int rowNumber) {
    List<String> parts =
        identityFields.stream()
            .map(field -> row.path(field).asText(""))
            .filter(value -> !value.isBlank())
            .toList();
    if (!parts.isEmpty()) return String.join("|", parts);
    String date = row.path("TMPV1").asText("");
    return date.matches("\\d{8}")
        ? LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE).toString()
        : "ROW-" + rowNumber;
  }

  public String entityCode(JsonNode row) {
    for (String field : List.of("TMPV98", "TMPV2")) {
      String value = row.path(field).asText("");
      if (!value.isBlank()) return value;
    }
    return null;
  }

  public String entityName(JsonNode row) {
    String value = row.path("TMPV1").asText("");
    return value.matches("\\d{8}") || value.isBlank() ? null : value;
  }

  public static KofiaDataset fromCode(String code) {
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("KOFIA Dataset 코드가 필요합니다.");
    String value = code.trim().toUpperCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(
            dataset ->
                dataset.name().equals(value)
                    || dataset.serviceId.equals(value)
                    || dataset.objectName.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 KOFIA Dataset입니다: " + code));
  }
}
