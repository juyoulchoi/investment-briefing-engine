package com.nanum.investment.regularbuy.domain;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public enum WeekDayCode {
  MON(DayOfWeek.MONDAY),
  TUE(DayOfWeek.TUESDAY),
  WED(DayOfWeek.WEDNESDAY),
  THU(DayOfWeek.THURSDAY),
  FRI(DayOfWeek.FRIDAY),
  SAT(DayOfWeek.SATURDAY),
  SUN(DayOfWeek.SUNDAY);

  private final DayOfWeek dayOfWeek;

  WeekDayCode(DayOfWeek dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public DayOfWeek dayOfWeek() {
    return dayOfWeek;
  }

  public static Set<WeekDayCode> parse(String codes) {
    if (codes == null || codes.isBlank()) return Set.of();
    EnumSet<WeekDayCode> result = EnumSet.noneOf(WeekDayCode.class);
    Arrays.stream(codes.split(","))
        .map(String::trim)
        .map(String::toUpperCase)
        .map(WeekDayCode::valueOf)
        .forEach(result::add);
    return result;
  }
}
