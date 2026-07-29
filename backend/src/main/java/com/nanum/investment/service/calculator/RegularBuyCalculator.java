package com.nanum.investment.service.calculator;

import com.nanum.investment.domain.RegularBuySignal;
import com.nanum.investment.domain.RiskLevel;
import com.nanum.investment.domain.WeightStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class RegularBuyCalculator {
    public RegularBuySignal calculate(LocalDate date, boolean marketOpen, String cycle,
            boolean paused, WeightStatus weightStatus, RiskLevel riskLevel,
            BigDecimal availableCash, BigDecimal regularAmount) {
        if (paused) return RegularBuySignal.PAUSED;
        if (!marketOpen || !isScheduledDate(date, cycle)) return RegularBuySignal.NOT_SCHEDULED;
        if (weightStatus == WeightStatus.OVER) return RegularBuySignal.OVER_WEIGHT;
        if (availableCash == null || regularAmount == null || availableCash.compareTo(regularAmount) < 0)
            return RegularBuySignal.INSUFFICIENT_CASH;
        if (riskLevel == RiskLevel.VERY_HIGH) return RegularBuySignal.RISK_BLOCKED;
        return RegularBuySignal.EXECUTE;
    }

    private boolean isScheduledDate(LocalDate date, String cycle) {
        if (date == null || cycle == null) return false;
        return switch (cycle) {
            case "DAILY" -> true;
            case "MONDAY" -> date.getDayOfWeek() == DayOfWeek.MONDAY;
            case "FRIDAY" -> date.getDayOfWeek() == DayOfWeek.FRIDAY;
            case "MON_WED_FRI" -> date.getDayOfWeek() == DayOfWeek.MONDAY
                    || date.getDayOfWeek() == DayOfWeek.WEDNESDAY
                    || date.getDayOfWeek() == DayOfWeek.FRIDAY;
            case "MONTHLY_15" -> date.getDayOfMonth() == 15;
            default -> false;
        };
    }
}
