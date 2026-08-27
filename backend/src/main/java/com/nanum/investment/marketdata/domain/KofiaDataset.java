package com.nanum.investment.marketdata.domain;

import java.util.Arrays;
import java.util.Locale;

public enum KofiaDataset {
  CREDIT_BALANCE_TREND(
      "STATSCU0100000070",
      "STATSCU0100000070BO",
      "주식 > 신용공여현황 > 신용공여 잔고 추이",
      "/meta/getMetaDataList.do");

  private final String serviceId;
  private final String objectName;
  private final String description;
  private final String path;

  KofiaDataset(String serviceId, String objectName, String description, String path) {
    this.serviceId = serviceId;
    this.objectName = objectName;
    this.description = description;
    this.path = path;
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
    return path;
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
