package com.nanum.investment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketCalendarService {
  private final JdbcClient jdbcClient;

  public boolean isMarketOpen(LocalDate date, String marketCode) {
    if (date == null) return false;
    String resolved = marketCode == null || marketCode.isBlank() ? "KRX" : marketCode.toUpperCase();
    return jdbcClient
        .sql(
            """
                        SELECT open_yn FROM tb_mkt_cal
                         WHERE cal_dt = :date AND market_cd = :marketCode
                        """)
        .param("date", date)
        .param("marketCode", resolved)
        .query(String.class)
        .optional()
        .map("Y"::equalsIgnoreCase)
        .orElseGet(
            () ->
                date.getDayOfWeek() != DayOfWeek.SATURDAY
                    && date.getDayOfWeek() != DayOfWeek.SUNDAY);
  }
}
