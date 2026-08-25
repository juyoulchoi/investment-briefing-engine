package com.nanum.investment.marketdata.domain;

import java.util.Arrays;
import java.util.List;

public enum KrxDataset {
  KOSPI_STOCK_MASTER("/svc/apis/sto/stk_isu_base_info", List.of("ISU_CD", "ISU_SRT_CD")),
  KOSDAQ_STOCK_MASTER("/svc/apis/sto/ksq_isu_base_info", List.of("ISU_CD", "ISU_SRT_CD")),
  KOSPI_STOCK_DAILY("/svc/apis/sto/stk_bydd_trd", List.of("ISU_CD", "ISU_SRT_CD")),
  KOSDAQ_STOCK_DAILY("/svc/apis/sto/ksq_bydd_trd", List.of("ISU_CD", "ISU_SRT_CD")),
  KOSPI_INDEX_DAILY("/svc/apis/idx/kospi_dd_trd", List.of("IDX_CLSS", "IDX_NM")),
  KOSDAQ_INDEX_DAILY("/svc/apis/idx/kosdaq_dd_trd", List.of("IDX_CLSS", "IDX_NM")),
  KRX_INDEX_DAILY("/svc/apis/idx/krx_dd_trd", List.of("IDX_CLSS", "IDX_NM")),
  DERIVATIVE_INDEX_DAILY("/svc/apis/idx/drvprod_dd_trd", List.of("IDX_CLSS", "IDX_NM")),
  FUTURES_DAILY("/svc/apis/drv/fut_bydd_trd", List.of("ISU_CD", "ISU_SRT_CD")),
  ETF_DAILY("/svc/apis/etp/etf_bydd_trd", List.of("ISU_CD", "ISU_SRT_CD"));

  private final String path;
  private final List<String> keys;

  KrxDataset(String path, List<String> keys) {
    this.path = path;
    this.keys = keys;
  }

  public String path() {
    return path;
  }

  public List<String> keys() {
    return keys;
  }

  public String apiMethod() {
    return path.substring(path.lastIndexOf('/') + 1);
  }

  public static KrxDataset fromApiMethod(String apiMethod) {
    return Arrays.stream(values())
        .filter(dataset -> dataset.apiMethod().equals(apiMethod))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 KRX API 메서드입니다: " + apiMethod));
  }
}
