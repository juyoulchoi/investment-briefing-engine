package com.nanum.investment.marketdata.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nanum.investment.common.exception.GlobalExceptionHandler;
import com.nanum.investment.marketdata.application.VolumeConsolidationService;
import com.nanum.investment.marketdata.domain.VolumeConsolidation.*;
import com.nanum.investment.marketdata.infrastructure.VolumeConsolidationRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VolumeConsolidationControllerTest {
  @Test
  void serializesNoDataAndRejectsInvalidDatesAsBadRequests() throws Exception {
    var repository = mock(VolumeConsolidationRepository.class);
    LocalDate date = LocalDate.of(2025, 1, 1);
    when(repository.coverage(date)).thenReturn(List.of());
    when(repository.dates(date, 75)).thenReturn(List.of());
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new VolumeConsolidationController(new VolumeConsolidationService(repository)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    mvc.perform(get("/api/v1/krx/screens/volume-consolidation").param("baseDate", "2025-01-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.availableDays").value(0))
        .andExpect(jsonPath("$.rules.version").value("VOLUME_CONSOLIDATION_V1"));
    mvc.perform(get("/api/v1/krx/screens/volume-consolidation").param("baseDate", "2999-01-01"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/krx/screens/volume-consolidation").param("baseDate", "invalid"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/krx/screens/volume-consolidation/tracking"))
        .andExpect(status().isBadRequest());
  }
}
