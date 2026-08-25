package com.nanum.investment.marketdata.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nanum.investment.marketdata.domain.KrxDataset;
import org.junit.jupiter.api.Test;

class KrxDatasetTest {
  @Test
  void resolvesDatasetByKrxApiMethodName() {
    assertEquals("stk_bydd_trd", KrxDataset.KOSPI_STOCK_DAILY.apiMethod());
    assertEquals(KrxDataset.KOSPI_STOCK_DAILY, KrxDataset.fromApiMethod("stk_bydd_trd"));
    assertEquals(KrxDataset.DERIVATIVE_INDEX_DAILY, KrxDataset.fromApiMethod("drvprod_dd_trd"));
    assertEquals(KrxDataset.FUTURES_DAILY, KrxDataset.fromApiMethod("fut_bydd_trd"));
    assertEquals(KrxDataset.BOND_INDEX_DAILY, KrxDataset.fromApiMethod("bon_dd_trd"));
    assertEquals(KrxDataset.KONEX_STOCK_DAILY, KrxDataset.fromApiMethod("knx_bydd_trd"));
    assertEquals(KrxDataset.SUBSCRIPTION_WARRANT_DAILY, KrxDataset.fromApiMethod("sw_bydd_trd"));
    assertEquals(KrxDataset.SUBSCRIPTION_RIGHT_DAILY, KrxDataset.fromApiMethod("sr_bydd_trd"));
    assertEquals(KrxDataset.ALL_STOCK_MASTER, KrxDataset.fromApiMethod("isu_base_info"));
    assertEquals(KrxDataset.KONEX_STOCK_MASTER, KrxDataset.fromApiMethod("knx_isu_base_info"));
  }

  @Test
  void rejectsUnknownApiMethodName() {
    assertThrows(IllegalArgumentException.class, () -> KrxDataset.fromApiMethod("unknown"));
  }
}
