package com.nanum.investment.marketdata.application;

import com.nanum.investment.common.infrastructure.external.CollectionResult;
import com.nanum.investment.common.application.CommonCodeLookupService;
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
  private static final String SERIES_GROUP = "BOND_YIELD_SERIES";
  private final FredBondYieldCollector collector;
  private final JdbcClient jdbc;
  private final CommonCodeLookupService commonCodes;

  public FredBondYieldService(
      FredBondYieldCollector collector, JdbcClient jdbc, CommonCodeLookupService commonCodes) {
    this.collector = collector;
    this.jdbc = jdbc;
    this.commonCodes = commonCodes;
  }

  public RefreshResult refreshLatest() {
    LocalDate to = LocalDate.now(ZoneId.of("Asia/Seoul"));
    LocalDate from = to.minusDays(14);
    CollectionResult collection = collect(from, to);
    LocalDate latest =
        jdbc.sql(
                """
                SELECT MAX(b."BASE_DT")
                  FROM "TB_BOND_DAY" b
                  JOIN "TB_CD_DTL" c
                    ON c."CD_GRP"=:group AND c."CD_KEY"=b."BOND_CD" AND c."ACTV_YN"='Y'
                 WHERE c."CD_KEY"<>'ALL'
                """)
            .param("group", SERIES_GROUP)
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
    List<String> seriesCodes =
        commonCodes.activeCodes(SERIES_GROUP).stream()
            .map(CommonCodeLookupService.CommonCode::code)
            .filter(code -> !"ALL".equals(code))
            .toList();
    if (seriesCodes.isEmpty())
      throw new IllegalStateException("수집할 활성 채권금리 공통코드가 없습니다.");
    for (String code : seriesCodes) {
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
  SELECT b."BASE_DT" base_date,b."BOND_CD" bond_code,c."CD_NM" bond_name,b."CNTRY_CD" country_code,
   b."MATURITY_MON" maturity_months,b."YLD_RT" yield_rate,b."PREV_YLD_RT" previous_yield_rate,
   b."CHG_BP" change_basis_points,b."DATA_SRC_CD" data_source_code,b."DATA_STS" data_status
  FROM "TB_BOND_DAY" b
  JOIN "TB_CD_DTL" c
    ON c."CD_GRP"=:group AND c."CD_KEY"=b."BOND_CD" AND c."ACTV_YN"='Y'
  WHERE b."BASE_DT" BETWEEN :from AND :to
  ORDER BY b."BASE_DT" DESC,c."DSP_ORD",b."BOND_CD"
  """)
        .param("group", SERIES_GROUP)
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
