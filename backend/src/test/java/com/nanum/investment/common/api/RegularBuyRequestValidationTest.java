package com.nanum.investment.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.nanum.investment.common.api.OperationalDataAdminController.RegularBuyRequest;
import com.nanum.investment.regularbuy.domain.BuyCycle;
import com.nanum.investment.regularbuy.domain.RegularBuyStatus;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegularBuyRequestValidationTest {
  @ParameterizedTest
  @CsvSource({
    "ACTIVE,N,WEEKLY,false", "ACTIVE,N,MONTHLY,false",
    "ACTIVE,Y,WEEKLY,true", "ACTIVE,Y,MONTHLY,true",
    "STOPPED,N,WEEKLY,true", "STOPPED,N,MONTHLY,true",
    "STOPPED,Y,WEEKLY,true", "STOPPED,Y,MONTHLY,true",
    "PAUSED,N,WEEKLY,false", "PAUSED,N,MONTHLY,false", "ACTIVE,N,DAILY,true"
  })
  void validatesMissingScheduleForEveryStopAndPauseCombination(
      RegularBuyStatus status, String pause, String cycle, boolean valid) {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var violations = factory.getValidator().validate(request(status, pause, cycle, null, null));
      assertThat(violations.isEmpty()).isEqualTo(valid);
      if (!valid) {
        assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).contains("선택해 주세요"));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"WEEKLY,TUE,", "MONTHLY,,15"})
  void acceptsSelectedSchedule(String cycle, String weekdays, String monthDays) {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(
          request(RegularBuyStatus.ACTIVE, "N", cycle, weekdays, monthDays))).isEmpty();
    }
  }

  private RegularBuyRequest request(
      RegularBuyStatus status, String pause, String cycle, String weekdays, String monthDays) {
    return new RegularBuyRequest(3L, 23L, 1, null, null, BuyCycle.WEEKLY, "TUE", null,
        null, cycle, weekdays, monthDays, BigDecimal.ZERO, "QUANTITY", BigDecimal.ZERO,
        BigDecimal.ONE, BigDecimal.ONE, status, null, pause, "Y");
  }
}
