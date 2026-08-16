package com.nanum.investment.marketdata.application;

import com.nanum.investment.common.infrastructure.external.CollectionResult;
import com.nanum.investment.marketdata.infrastructure.BondYieldCollector;
import com.nanum.investment.marketdata.infrastructure.FredBondYieldCollector;
import java.math.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.slf4j.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class FredBondYieldService {
  private static final Logger log = LoggerFactory.getLogger(FredBondYieldService.class);
  private static final List<String> CODES = List.of("DGS2", "DGS10", "DGS30", "DFII10");
  private final FredBondYieldCollector collector;
  private final JdbcClient jdbc;

  public FredBondYieldService(FredBondYieldCollector collector, JdbcClient jdbc) {
    this.collector = collector;
    this.jdbc = jdbc;
  }

  public RefreshResult refreshLatest() {
    LocalDate to = LocalDate.now(ZoneId.of("Asia/Seoul"));
    LocalDate from = to.minusDays(14);
    CollectionResult collection = collect(from, to);
    LocalDate latest =
        jdbc.sql(
                "SELECT MAX(\"BASE_DT\") FROM \"TB_BOND_DAY\" WHERE \"BOND_CD\" IN ('DGS2','DGS10','DGS30','DFII10')")
            .query(LocalDate.class)
            .optional()
            .orElse(null);
    return new RefreshResult(from, to, collection.savedCount(), collection.seriesCounts(), latest);
  }

  public CollectionResult collect(LocalDate from, LocalDate to) {
    if (from == null || to == null || from.isAfter(to))
      throw new IllegalArgumentException("유효하지 않은 조회 기간입니다.");
    int saved = 0;
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String code : CODES) {
      int count = 0;
      for (BondYieldCollector.Yield value : collector.collectRange(code, from, to)) {
        save(value);
        count++;
        saved++;
      }
      counts.put(code, count);
    }
    return new CollectionResult(from, to, saved, counts);
  }

  public List<Map<String, Object>> history(LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
  SELECT "BASE_DT" base_date,"BOND_CD" bond_code,"BOND_NM" bond_name,"CNTRY_CD" country_code,
   "MATURITY_MON" maturity_months,"YLD_RT" yield_rate,"PREV_YLD_RT" previous_yield_rate,
   "CHG_BP" change_basis_points,"DATA_SRC_CD" data_source_code,"DATA_STS" data_status
  FROM "TB_BOND_DAY" WHERE "BASE_DT" BETWEEN :from AND :to ORDER BY "BASE_DT" DESC,"BOND_CD"
  """)
        .param("from", from)
        .param("to", to)
        .query()
        .listOfRows();
  }

  private void save(BondYieldCollector.Yield value) {
    BigDecimal previous =
        jdbc.sql(
                "SELECT \"YLD_RT\" FROM \"TB_BOND_DAY\" WHERE \"BOND_CD\"=:code AND \"BASE_DT\"<:day ORDER BY \"BASE_DT\" DESC LIMIT 1")
            .param("code", value.bondCode())
            .param("day", value.baseDate())
            .query(BigDecimal.class)
            .optional()
            .orElse(null);
    BigDecimal bp =
        previous == null
            ? null
            : value
                .yieldRate()
                .subtract(previous)
                .multiply(new BigDecimal("100"))
                .setScale(4, RoundingMode.HALF_UP);
    jdbc.sql(
            """
  INSERT INTO "TB_BOND_DAY"("BASE_DT","BOND_CD","BOND_NM","CNTRY_CD","MATURITY_MON","YLD_RT","PREV_YLD_RT","CHG_BP","DATA_SRC_CD","DATA_STS")
  VALUES(:day,:code,:name,:country,:months,:rate,:previous,:bp,'FRED','FRESH')
  ON CONFLICT("BASE_DT","BOND_CD") DO UPDATE SET "BOND_NM"=EXCLUDED."BOND_NM","CNTRY_CD"=EXCLUDED."CNTRY_CD","MATURITY_MON"=EXCLUDED."MATURITY_MON","YLD_RT"=EXCLUDED."YLD_RT","PREV_YLD_RT"=EXCLUDED."PREV_YLD_RT","CHG_BP"=EXCLUDED."CHG_BP","DATA_SRC_CD"='FRED',"DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP
  """)
        .param("day", value.baseDate())
        .param("code", value.bondCode())
        .param("name", value.bondName())
        .param("country", value.countryCode())
        .param("months", value.maturityMonths())
        .param("rate", value.yieldRate())
        .param("previous", previous)
        .param("bp", bp)
        .update();
  }

  public record CollectionResult(
      LocalDate from, LocalDate to, int savedCount, Map<String, Integer> seriesCounts) {}

  public record RefreshResult(
      LocalDate from,
      LocalDate to,
      int savedCount,
      Map<String, Integer> seriesCounts,
      LocalDate latestObservationDate) {}
}
