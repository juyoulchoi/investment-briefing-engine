package com.nanum.investment.marketdata;

import com.nanum.investment.domain.DataStatus;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class MarketDataConsistencyService {
  private static final List<String> KRX_DATASETS =
      List.of("KOSPI_STOCK_DAILY", "KOSDAQ_STOCK_DAILY", "ETF_DAILY");
  private static final List<String> BOND_CODES = List.of("DGS2", "DGS10", "DGS30", "DFII10");
  private final JdbcClient jdbc;

  public MarketDataConsistencyService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public MarketDataValidationResult validate(LocalDate baseDate) {
    if (baseDate == null) throw new IllegalArgumentException("검증 기준일이 필요합니다.");
    List<MarketDataValidationResult.ComponentResult> components =
        List.of(
            validateKrx(baseDate),
            validateIndices(baseDate),
            validateOverseas(baseDate),
            validateExchange(baseDate),
            validateBonds(baseDate));
    List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
    for (var component : components) {
      component.errors().forEach(x -> errors.add("[" + component.name() + "] " + x));
      component.warnings().forEach(x -> warnings.add("[" + component.name() + "] " + x));
    }
    validateDateAlignment(components, errors, warnings);
    boolean valid = errors.isEmpty();
    DataStatus status =
        !valid ? DataStatus.ERROR : warnings.isEmpty() ? DataStatus.FRESH : DataStatus.PARTIAL;
    int confidence = Math.max(0, 100 - errors.size() * 25 - warnings.size() * 5);
    return new MarketDataValidationResult(
        baseDate,
        OffsetDateTime.now(ZoneId.of("Asia/Seoul")),
        valid,
        status,
        confidence,
        components,
        List.copyOf(errors),
        List.copyOf(warnings));
  }

  public MarketDataValidationResult validateOrThrow(LocalDate baseDate) {
    MarketDataValidationResult result = validate(baseDate);
    if (!result.valid())
      throw new IllegalStateException("시장데이터 정합성 검증 실패: " + String.join("; ", result.errors()));
    return result;
  }

  private MarketDataValidationResult.ComponentResult validateKrx(LocalDate baseDate) {
    List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
    LocalDate latest = null;
    long total = 0;
    for (String dataset : KRX_DATASETS) {
      Snapshot value =
          jdbc.sql(
                  "SELECT max(base_date),count(*) FILTER(WHERE base_date=(SELECT max(base_date) FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date<=:day)) FROM tb_krx_data_row WHERE dataset_code=:dataset AND base_date<=:day")
              .param("dataset", dataset)
              .param("day", baseDate)
              .query((rs, n) -> new Snapshot(rs.getObject(1, LocalDate.class), rs.getLong(2)))
              .single();
      if (value.date() == null || value.count() == 0) errors.add(dataset + " 데이터가 없습니다.");
      else {
        total += value.count();
        latest = max(latest, value.date());
        long age = age(value.date(), baseDate);
        if (age > 5) errors.add(dataset + " 최신 데이터가 " + age + "일 지연되었습니다.");
        else if (age > 3) warnings.add(dataset + " 최신 데이터가 " + age + "일 지연되었습니다.");
      }
    }
    Long invalid =
        jdbc.sql(
                """
   SELECT count(*) FROM tb_krx_data_row WHERE base_date=(SELECT max(base_date) FROM tb_krx_data_row WHERE base_date<=:day)
    AND dataset_code IN ('KOSPI_STOCK_DAILY','KOSDAQ_STOCK_DAILY','ETF_DAILY') AND
    (NULLIF(replace(payload->>'TDD_CLSPRC',',',''),'')::numeric<=0 OR NULLIF(replace(payload->>'ACC_TRDVOL',',',''),'')::numeric<0)
   """)
            .param("day", baseDate)
            .query(Long.class)
            .single();
    if (invalid > 0) errors.add("종가 또는 거래량 범위가 잘못된 행이 " + invalid + "건입니다.");
    return component("KR_MARKET", "한국시장", latest, total, errors, warnings);
  }

  private MarketDataValidationResult.ComponentResult validateOverseas(LocalDate baseDate) {
    List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
    List<LatestRow> stocks =
        jdbc.sql(
                """
   SELECT s."STK_CD",p."TRADE_DT",p."CLS_PRC",p."CHG_RT",p."DATA_STS" FROM "TB_STK" s
   LEFT JOIN LATERAL(SELECT "TRADE_DT","CLS_PRC","CHG_RT","DATA_STS" FROM "TB_PRC_DAY" d WHERE d."STK_ID"=s."STK_ID" AND d."TRADE_DT"<=:day ORDER BY d."TRADE_DT" DESC LIMIT 1)p ON true
   WHERE s."LIST_SCOPE"='OVERSEAS' AND s."USE_YN"='Y' AND s."DEL_YN"='N'
   """)
            .param("day", baseDate)
            .query(
                (rs, n) ->
                    new LatestRow(
                        rs.getString(1),
                        rs.getObject(2, LocalDate.class),
                        rs.getBigDecimal(3),
                        rs.getBigDecimal(4),
                        rs.getString(5)))
            .list();
    LocalDate latest = null;
    for (LatestRow row : stocks) {
      if (row.date() == null) {
        errors.add(row.code() + " 종가가 없습니다.");
        continue;
      }
      latest = max(latest, row.date());
      long age = age(row.date(), baseDate);
      if (age > 5) errors.add(row.code() + " 종가가 " + age + "일 지연되었습니다.");
      else if (age > 3) warnings.add(row.code() + " 종가가 " + age + "일 지연되었습니다.");
      if (row.value() == null || row.value().signum() <= 0)
        errors.add(row.code() + " 종가가 유효하지 않습니다.");
      if (row.change() != null && row.change().abs().compareTo(new BigDecimal("30")) > 0)
        warnings.add(row.code() + " 일간 등락률이 30%를 초과했습니다.");
      if (row.status() != null && !"FRESH".equals(row.status()))
        errors.add(row.code() + " 데이터 상태가 " + row.status() + "입니다.");
    }
    Long invalidOhlc =
        jdbc.sql(
                """
   SELECT count(*) FROM "TB_PRC_DAY" d JOIN "TB_STK" s ON s."STK_ID"=d."STK_ID" WHERE s."LIST_SCOPE"='OVERSEAS' AND d."TRADE_DT"=(SELECT max(x."TRADE_DT") FROM "TB_PRC_DAY" x WHERE x."STK_ID"=d."STK_ID" AND x."TRADE_DT"<=:day)
    AND (d."HIGH_PRC"<d."LOW_PRC" OR d."HIGH_PRC"<d."OPEN_PRC" OR d."HIGH_PRC"<d."CLS_PRC" OR d."LOW_PRC">d."OPEN_PRC" OR d."LOW_PRC">d."CLS_PRC" OR d."VOL"<0)
   """)
            .param("day", baseDate)
            .query(Long.class)
            .single();
    if (invalidOhlc > 0) errors.add("OHLC 또는 거래량 정합성이 잘못된 종목이 " + invalidOhlc + "건입니다.");
    if (stocks.isEmpty()) errors.add("검증할 활성 해외종목이 없습니다.");
    return component("OVERSEAS_MARKET", "해외시장", latest, stocks.size(), errors, warnings);
  }

  private MarketDataValidationResult.ComponentResult validateIndices(LocalDate baseDate) {
    List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
    List<LatestRow> rows = new ArrayList<>();
    LatestRow kospi =
        jdbc.sql(
                """
   SELECT 'KOSPI',base_date,NULLIF(replace(payload->>'CLSPRC_IDX',',',''),'')::numeric,NULL::numeric,'FRESH'
   FROM tb_krx_data_row WHERE dataset_code='KOSPI_INDEX_DAILY' AND base_date<=:day
    AND payload->>'IDX_NM' IN ('코스피','KOSPI') ORDER BY base_date DESC LIMIT 1
   """)
            .param("day", baseDate)
            .query(
                (rs, n) ->
                    new LatestRow(
                        rs.getString(1),
                        rs.getObject(2, LocalDate.class),
                        rs.getBigDecimal(3),
                        rs.getBigDecimal(4),
                        rs.getString(5)))
            .optional()
            .orElse(null);
    if (kospi != null) rows.add(kospi);
    rows.addAll(
        jdbc.sql(
                """
   SELECT i."IDX_CD",d."TRADE_DT",d."CLS_VAL",d."CHG_RT",d."DATA_STS" FROM "TB_IDX" i
   LEFT JOIN LATERAL(SELECT "TRADE_DT","CLS_VAL","CHG_RT","DATA_STS" FROM "TB_IDX_DAY" x
    WHERE x."IDX_ID"=i."IDX_ID" AND x."TRADE_DT"<=:day ORDER BY x."TRADE_DT" DESC LIMIT 1)d ON true
   WHERE i."IDX_CD" IN ('SP500','VIX') ORDER BY i."IDX_CD"
   """)
            .param("day", baseDate)
            .query(
                (rs, n) ->
                    new LatestRow(
                        rs.getString(1),
                        rs.getObject(2, LocalDate.class),
                        rs.getBigDecimal(3),
                        rs.getBigDecimal(4),
                        rs.getString(5)))
            .list());
    for (String code : List.of("KOSPI", "SP500", "VIX")) {
      LatestRow row = rows.stream().filter(x -> code.equals(x.code())).findFirst().orElse(null);
      if (row == null || row.date() == null) {
        errors.add(code + " 대표지수가 없습니다.");
        continue;
      }
      long age = age(row.date(), baseDate);
      if (age > 5) errors.add(code + " 대표지수가 " + age + "일 지연되었습니다.");
      else if (age > 3) warnings.add(code + " 대표지수가 " + age + "일 지연되었습니다.");
      if (row.value() == null || row.value().signum() <= 0)
        errors.add(code + " 대표지수 값이 유효하지 않습니다.");
    }
    LocalDate latest =
        rows.stream()
            .map(LatestRow::date)
            .filter(Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);
    return component(
        "REPRESENTATIVE_INDEX",
        "대표지수",
        latest,
        rows.stream().filter(x -> x.date() != null).count(),
        errors,
        warnings);
  }

  private MarketDataValidationResult.ComponentResult validateExchange(LocalDate baseDate) {
    List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
    LatestRow row =
        jdbc.sql(
                """
   SELECT 'USD/KRW',"BASE_DT","EXCH_RT","CHG_RT","DATA_STS" FROM "TB_EXCH_DAY" WHERE "BASE_CURR_CD"='USD' AND "QUOTE_CURR_CD"='KRW' AND "BASE_DT"<=:day ORDER BY "BASE_DT" DESC LIMIT 1
   """)
            .param("day", baseDate)
            .query(
                (rs, n) ->
                    new LatestRow(
                        rs.getString(1),
                        rs.getObject(2, LocalDate.class),
                        rs.getBigDecimal(3),
                        rs.getBigDecimal(4),
                        rs.getString(5)))
            .optional()
            .orElse(null);
    if (row == null) errors.add("USD/KRW 환율이 없습니다.");
    else {
      long age = age(row.date(), baseDate);
      if (age > 5) errors.add("USD/KRW 환율이 " + age + "일 지연되었습니다.");
      else if (age > 3) warnings.add("USD/KRW 환율이 " + age + "일 지연되었습니다.");
      if (row.value() == null
          || row.value().compareTo(new BigDecimal("500")) < 0
          || row.value().compareTo(new BigDecimal("3000")) > 0)
        errors.add("USD/KRW 환율이 허용범위(500~3000)를 벗어났습니다.");
      if (row.change() != null && row.change().abs().compareTo(new BigDecimal("5")) > 0)
        warnings.add("USD/KRW 일간 변동률이 5%를 초과했습니다.");
      if (!"FRESH".equals(row.status())) errors.add("USD/KRW 데이터 상태가 " + row.status() + "입니다.");
    }
    return component(
        "EXCHANGE_RATE",
        "환율",
        row == null ? null : row.date(),
        row == null ? 0 : 1,
        errors,
        warnings);
  }

  private MarketDataValidationResult.ComponentResult validateBonds(LocalDate baseDate) {
    List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
    LocalDate latest = null;
    long count = 0;
    for (String code : BOND_CODES) {
      LatestRow row =
          jdbc.sql(
                  "SELECT \"BOND_CD\",\"BASE_DT\",\"YLD_RT\",\"CHG_BP\",\"DATA_STS\" FROM \"TB_BOND_DAY\" WHERE \"BOND_CD\"=:code AND \"BASE_DT\"<=:day ORDER BY \"BASE_DT\" DESC LIMIT 1")
              .param("code", code)
              .param("day", baseDate)
              .query(
                  (rs, n) ->
                      new LatestRow(
                          rs.getString(1),
                          rs.getObject(2, LocalDate.class),
                          rs.getBigDecimal(3),
                          rs.getBigDecimal(4),
                          rs.getString(5)))
              .optional()
              .orElse(null);
      if (row == null) {
        errors.add(code + " 금리가 없습니다.");
        continue;
      }
      count++;
      latest = max(latest, row.date());
      long age = age(row.date(), baseDate);
      if (age > 7) errors.add(code + " 금리가 " + age + "일 지연되었습니다.");
      else if (age > 4) warnings.add(code + " 금리가 " + age + "일 지연되었습니다.");
      if (row.value() == null
          || row.value().compareTo(new BigDecimal("-5")) < 0
          || row.value().compareTo(new BigDecimal("25")) > 0)
        errors.add(code + " 금리가 허용범위(-5~25%)를 벗어났습니다.");
      if (row.change() != null && row.change().abs().compareTo(new BigDecimal("100")) > 0)
        warnings.add(code + " 일간 변동이 100bp를 초과했습니다.");
      if (!"FRESH".equals(row.status())) errors.add(code + " 데이터 상태가 " + row.status() + "입니다.");
    }
    return component("BOND_YIELD", "채권금리", latest, count, errors, warnings);
  }

  private void validateDateAlignment(
      List<MarketDataValidationResult.ComponentResult> values,
      List<String> errors,
      List<String> warnings) {
    List<LocalDate> dates =
        values.stream()
            .map(MarketDataValidationResult.ComponentResult::latestDataDate)
            .filter(Objects::nonNull)
            .sorted()
            .toList();
    if (dates.size() < 2) return;
    long gap = ChronoUnit.DAYS.between(dates.getFirst(), dates.getLast());
    if (gap > 7) errors.add("시장데이터 기준일 간격이 " + gap + "일입니다.");
    else if (gap > 4) warnings.add("시장데이터 기준일 간격이 " + gap + "일입니다.");
  }

  private MarketDataValidationResult.ComponentResult component(
      String code,
      String name,
      LocalDate date,
      long count,
      List<String> errors,
      List<String> warnings) {
    boolean valid = errors.isEmpty();
    DataStatus status =
        !valid ? DataStatus.ERROR : warnings.isEmpty() ? DataStatus.FRESH : DataStatus.PARTIAL;
    return new MarketDataValidationResult.ComponentResult(
        code, name, valid, status, date, count, List.copyOf(errors), List.copyOf(warnings));
  }

  private long age(LocalDate date, LocalDate baseDate) {
    return Math.max(0, ChronoUnit.DAYS.between(date, baseDate));
  }

  private LocalDate max(LocalDate left, LocalDate right) {
    return left == null ? right : left.isAfter(right) ? left : right;
  }

  private record Snapshot(LocalDate date, long count) {}

  private record LatestRow(
      String code, LocalDate date, BigDecimal value, BigDecimal change, String status) {}
}
