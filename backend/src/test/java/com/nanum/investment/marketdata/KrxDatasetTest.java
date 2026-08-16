package com.nanum.investment.marketdata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KrxDatasetTest {
    @Test
    void resolvesDatasetByKrxApiMethodName() {
        assertEquals("stk_bydd_trd", KrxDataset.KOSPI_STOCK_DAILY.apiMethod());
        assertEquals(KrxDataset.KOSPI_STOCK_DAILY, KrxDataset.fromApiMethod("stk_bydd_trd"));
    }

    @Test
    void rejectsUnknownApiMethodName() {
        assertThrows(IllegalArgumentException.class, () -> KrxDataset.fromApiMethod("unknown"));
    }
}
