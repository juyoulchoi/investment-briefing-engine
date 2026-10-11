package com.nanum.investment.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanum.investment.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RegularBuySaveValidationTest {
  @ParameterizedTest
  @CsvSource({
    "WEEKLY,매주 매수를 적용하려면 현재 매수요일을 선택해 주세요.",
    "MONTHLY,매월 매수를 적용하려면 현재 매수일을 선택해 주세요."
  })
  void returnsActionableBadRequestBeforeAccessingRepositories(String cycle, String message)
      throws Exception {
    var controller = new OperationalDataAdminController(
        null, null, null, null, null, null, null, null, null, null);
    var mvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    mvc.perform(put("/api/v1/admin/operations/regular-buys/PENSION/069500")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"accountId":3,"stockId":23,"buyCycle":"WEEKLY","appliedCycle":"%s",
             "buyBasis":"QUANTITY","minimumBuyAmount":10000,
             "buyStatus":"ACTIVE","userPauseYn":"N"}
            """.formatted(cycle)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.message").value(message))
        .andExpect(jsonPath("$.error.code").value("COMMON-400-001"));
  }
}
