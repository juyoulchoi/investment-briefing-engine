package com.nanum.collector;

import com.nanum.investment.marketdata.application.ExchangeRateService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "EXCHANGE_SHARED_COLLECTION_ENABLED", havingValue = "true")
public class CollectorExchangeScheduler {
  private final ExchangeRateService exchange;
  public CollectorExchangeScheduler(ExchangeRateService exchange) { this.exchange = exchange; }
  @Scheduled(cron = "${EXCHANGE_SHARED_CRON:0 0 8 * * MON-SAT}", zone = "Asia/Seoul")
  public void collect() {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    exchange.collect(today.minusDays(14), today);
  }
}
