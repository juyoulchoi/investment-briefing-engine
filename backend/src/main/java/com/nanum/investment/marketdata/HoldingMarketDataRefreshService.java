package com.nanum.investment.marketdata;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class HoldingMarketDataRefreshService {
  private static final List<KrxDataset> KRX_DATASETS =
      List.of(
          KrxDataset.KOSPI_STOCK_DAILY,
          KrxDataset.KOSDAQ_STOCK_DAILY,
          KrxDataset.ETF_DAILY,
          KrxDataset.KOSPI_INDEX_DAILY);
  private final KrxMarketDataService krx;
  private final OverseasStockService overseas;
  private final YahooIndexService indices;
  private final JdbcClient jdbc;

  public HoldingMarketDataRefreshService(
      KrxMarketDataService krx,
      OverseasStockService overseas,
      YahooIndexService indices,
      JdbcClient jdbc) {
    this.krx = krx;
    this.overseas = overseas;
    this.indices = indices;
    this.jdbc = jdbc;
  }

  public HoldingMarketDataRefreshResult refresh() {
    List<String> failures = new ArrayList<>();
    Map<String, Integer> krxCounts = new LinkedHashMap<>();
    LocalDate krxDate = refreshKrx(krxCounts, failures);
    List<String> symbols = overseasSymbols(), successes = new ArrayList<>();
    for (String symbol : symbols)
      try {
        overseas.refresh(symbol);
        successes.add(symbol);
      } catch (Exception e) {
        failures.add("해외 " + symbol + ": " + e.getMessage());
      }
    for (String index : List.of("SP500", "VIX"))
      try {
        indices.refresh(index);
      } catch (Exception e) {
        failures.add("지수 " + index + ": " + e.getMessage());
      }
    boolean krxComplete =
        krxDate != null
            && KRX_DATASETS.stream().allMatch(d -> krxCounts.getOrDefault(d.name(), 0) > 0);
    return new HoldingMarketDataRefreshResult(
        krxComplete && failures.isEmpty(),
        krxDate,
        Map.copyOf(krxCounts),
        symbols.size(),
        successes.size(),
        List.copyOf(successes),
        List.copyOf(failures));
  }

  private LocalDate refreshKrx(Map<String, Integer> counts, List<String> failures) {
    LocalDate candidate = latestCompletedKrxDate();
    String lastError = null;
    for (int attempt = 0; attempt < 8; attempt++, candidate = candidate.minusDays(1)) {
      if (candidate.getDayOfWeek() == DayOfWeek.SATURDAY
          || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
      Map<String, Integer> current = new LinkedHashMap<>();
      boolean complete = true;
      for (KrxDataset dataset : KRX_DATASETS)
        try {
          var result = krx.collect(dataset, candidate);
          current.put(dataset.name(), result.receivedCount());
          if (result.receivedCount() == 0) complete = false;
        } catch (Exception e) {
          lastError = e.getMessage();
          complete = false;
          break;
        }
      if (complete) {
        counts.putAll(current);
        return candidate;
      }
      if (lastError != null && lastError.contains("KRX_AUTH_KEY")) break;
    }
    failures.add("국내·ETF KRX 수집 실패" + (lastError == null ? " 또는 최근 거래일 데이터 없음" : ": " + lastError));
    return null;
  }

  private LocalDate latestCompletedKrxDate() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
    LocalDate date = now.toLocalDate();
    if (now.getHour() < 16) date = date.minusDays(1);
    while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
      date = date.minusDays(1);
    return date;
  }

  private List<String> overseasSymbols() {
    return jdbc.sql(
            """
  SELECT DISTINCT s."STK_CD" FROM "TB_HOLD" h JOIN "TB_STK" s ON s."STK_ID"=h."STK_ID"
  WHERE h."USE_YN"='Y' AND h."DEL_YN"='N' AND s."USE_YN"='Y' AND s."DEL_YN"='N'
   AND (s."MKT_CD"='US' OR s."CNTRY_CD"<>'KR') ORDER BY s."STK_CD"
  """)
        .query(String.class)
        .list();
  }
}
