package com.nanum.investment.marketdata;

import java.util.List;

public enum KrxDataset {
    KOSPI_STOCK_MASTER("/svc/apis/sto/stk_isu_base_info", List.of("ISU_CD", "ISU_SRT_CD")),
    KOSDAQ_STOCK_MASTER("/svc/apis/sto/ksq_isu_base_info", List.of("ISU_CD", "ISU_SRT_CD")),
    KOSPI_STOCK_DAILY("/svc/apis/sto/stk_bydd_trd", List.of("ISU_CD", "ISU_SRT_CD")),
    KOSDAQ_STOCK_DAILY("/svc/apis/sto/ksq_bydd_trd", List.of("ISU_CD", "ISU_SRT_CD")),
    KOSPI_INDEX_DAILY("/svc/apis/idx/kospi_dd_trd", List.of("IDX_CLSS", "IDX_NM")),
    KOSDAQ_INDEX_DAILY("/svc/apis/idx/kosdaq_dd_trd", List.of("IDX_CLSS", "IDX_NM")),
    KRX_INDEX_DAILY("/svc/apis/idx/krx_dd_trd", List.of("IDX_CLSS", "IDX_NM")),
    ETF_DAILY("/svc/apis/etp/etf_bydd_trd", List.of("ISU_CD", "ISU_SRT_CD"));

    private final String path;
    private final List<String> keys;
    KrxDataset(String path, List<String> keys) { this.path = path; this.keys = keys; }
    public String path() { return path; }
    public List<String> keys() { return keys; }
}
