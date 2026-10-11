package com.nanum.investment.marketdata.application;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nanum.investment.common.exception.GlobalExceptionHandler;
import com.nanum.investment.marketdata.api.ExchangeRateController;
import com.nanum.investment.marketdata.infrastructure.ExchangeRateSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ExchangeRateCollectionMovedTest {
  @Test
  void collectionMovedRemains409WithGlobalCatchAllAdvice() throws Exception {
    var service = mock(ExchangeRateService.class);
    var source = mock(ExchangeRateSource.class);
    when(source.shared()).thenReturn(true);
    var mvc = MockMvcBuilders.standaloneSetup(new ExchangeRateController(service, source))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    mvc.perform(post("/api/market-data/exchange-rates/collect")
        .param("from", "2026-10-01").param("to", "2026-10-09"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EXCHANGE_COLLECTION_MOVED"));
    verifyNoInteractions(service);
  }
}
