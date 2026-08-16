package com.nanum.investment.regularbuy.domain.calculator;

import com.nanum.investment.common.domain.RiskLevel;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.regularbuy.domain.RegularBuySignal;
import com.nanum.investment.regularbuy.domain.WeekDayCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class RegularBuyCalculator {
  public RegularBuySignal calculate(
      LocalDate date,
      boolean marketOpen,
      String cycle,
      String weekDays,
      Integer monthDay,
      boolean paused,
      WeightStatus weightStatus,
      RiskLevel riskLevel,
      BigDecimal availableCash,
      BigDecimal regularAmount) {
    if (paused) return RegularBuySignal.PAUSED;
    if (!marketOpen || !isScheduledDate(date, cycle, weekDays, monthDay))
      return RegularBuySignal.NOT_SCHEDULED;
    if (weightStatus == WeightStatus.OVER) return RegularBuySignal.OVER_WEIGHT;
    if (availableCash == null
        || regularAmount == null
        || availableCash.compareTo(regularAmount) < 0) return RegularBuySignal.INSUFFICIENT_CASH;
    if (riskLevel == RiskLevel.VERY_HIGH) return RegularBuySignal.RISK_BLOCKED;
    return RegularBuySignal.EXECUTE;
  }

  private boolean isScheduledDate(LocalDate date, String cycle, String weekDays, Integer monthDay) {
    if (date == null || cycle == null) return false;
    return switch (cycle) {
      case "DAILY" -> true;
      case "WEEKLY" ->
          WeekDayCode.parse(weekDays).stream()
              .anyMatch(code -> code.dayOfWeek() == date.getDayOfWeek());
      case "MONTHLY" -> monthDay != null && date.getDayOfMonth() == monthDay;
      default -> false;
    };
  }
}
