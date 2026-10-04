package com.nanum.investment.marketdata.infrastructure;

import static com.nanum.investment.marketdata.domain.VolumeConsolidation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class VolumeConsolidationRepository {
  private final JdbcClient jdbc;

  public VolumeConsolidationRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public List<LocalDate> dates(LocalDate cutoff, int limit) {
    // Index dates and known open calendar days help detect an entirely missing stock daily file.
    return jdbc
        .sql(
            """
        SELECT day FROM (
          SELECT DISTINCT "BASE_DT" AS day FROM "TB_KRX_DATA_ROW"
          WHERE "DATA_CD" IN ('KOSPI_STOCK_DAILY','KOSDAQ_STOCK_DAILY',
              'KOSPI_INDEX_DAILY','KOSDAQ_INDEX_DAILY') AND "BASE_DT" <= :cutoff
          UNION SELECT "CAL_DT" FROM "TB_MKT_CAL"
          WHERE "MKT_CD"='KRX' AND "OPEN_YN"='Y' AND "CAL_DT" <= :cutoff
        ) days ORDER BY day DESC LIMIT :limit
        """)
        .param("cutoff", cutoff)
        .param("limit", limit)
        .query(LocalDate.class)
        .list()
        .stream()
        .sorted()
        .toList();
  }

  public List<Coverage> coverage(LocalDate cutoff) {
    return jdbc.sql(
            """
        WITH markets(market, daily, master) AS (VALUES
          ('KOSPI','KOSPI_STOCK_DAILY','KOSPI_STOCK_MASTER'),
          ('KOSDAQ','KOSDAQ_STOCK_DAILY','KOSDAQ_STOCK_MASTER'))
        SELECT m.market, d.day AS latest, b.day AS master_date,
          (SELECT count(*) FROM "TB_KRX_DATA_ROW" r
           WHERE r."DATA_CD"=m.daily AND r."BASE_DT"=d.day) AS stock_count
        FROM markets m
        LEFT JOIN LATERAL (SELECT max("BASE_DT") AS day FROM "TB_KRX_DATA_ROW"
          WHERE "DATA_CD"=m.daily AND "BASE_DT"<=:cutoff) d ON true
        LEFT JOIN LATERAL (SELECT max("BASE_DT") AS day FROM "TB_KRX_DATA_ROW"
          WHERE "DATA_CD"=m.master AND "BASE_DT"<=:cutoff) b ON true
        ORDER BY m.market
        """)
        .param("cutoff", cutoff)
        .query(
            (rs, row) ->
                new Coverage(
                    rs.getString("market"),
                    rs.getObject("latest", LocalDate.class),
                    rs.getObject("master_date", LocalDate.class),
                    rs.getInt("stock_count")))
        .list();
  }

  public List<LocalDate> unverifiedWeekdays(LocalDate from, LocalDate to) {
    return jdbc.sql(
            """
        SELECT d.day::date FROM generate_series(CAST(:from AS timestamp),
          CAST(:to AS timestamp), interval '1 day') AS d(day)
        WHERE extract(isodow FROM d.day) <= 5
          AND NOT EXISTS (SELECT 1 FROM "TB_KRX_DATA_ROW" r
            WHERE r."BASE_DT"=d.day::date AND r."DATA_CD" IN
              ('KOSPI_STOCK_DAILY','KOSDAQ_STOCK_DAILY','KOSPI_INDEX_DAILY','KOSDAQ_INDEX_DAILY'))
          AND NOT EXISTS (SELECT 1 FROM "TB_MKT_CAL" c
            WHERE c."MKT_CD"='KRX' AND c."CAL_DT"=d.day::date AND c."OPEN_YN"='N')
        ORDER BY d.day
        """)
        .param("from", from)
        .param("to", to)
        .query(LocalDate.class)
        .list();
  }

  public Map<String, Master> masters(LocalDate cutoff) {
    Map<String, Master> result = new LinkedHashMap<>();
    jdbc.sql(
            """
        WITH latest AS (
          SELECT "DATA_CD", max("BASE_DT") AS day FROM "TB_KRX_DATA_ROW"
          WHERE "DATA_CD" IN ('KOSPI_STOCK_MASTER','KOSDAQ_STOCK_MASTER') AND "BASE_DT"<=:cutoff
          GROUP BY "DATA_CD")
        SELECT r."BASE_DT", r."DATA_CD", r."PAYLOAD"->>'ISU_SRT_CD' AS code,
          r."PAYLOAD"->>'KIND_STKCERT_TP_NM' AS kind, r."PAYLOAD"->>'SECUGRP_NM' AS security_group
        FROM "TB_KRX_DATA_ROW" r JOIN latest l ON r."DATA_CD"=l."DATA_CD" AND r."BASE_DT"=l.day
        """)
        .param("cutoff", cutoff)
        .query(
            (rs, row) -> {
              String market = rs.getString("DATA_CD").startsWith("KOSPI_") ? "KOSPI" : "KOSDAQ";
              result.put(
                  market + ":" + rs.getString("code"),
                  new Master(
                      rs.getObject("BASE_DT", LocalDate.class),
                      rs.getString("kind"),
                      rs.getString("security_group")));
              return 0;
            })
        .list();
    return result;
  }

  public List<Bar> bars(LocalDate from, LocalDate to, String code, String market) {
    return jdbc.sql(
            """
        SELECT "BASE_DT", "DATA_CD", "PAYLOAD"->>'ISU_CD' AS code,
          "PAYLOAD"->>'ISU_NM' AS name, "PAYLOAD"->>'SECT_TP_NM' AS section,
          "PAYLOAD"->>'TDD_OPNPRC' AS open, "PAYLOAD"->>'TDD_HGPRC' AS high,
          "PAYLOAD"->>'TDD_LWPRC' AS low, "PAYLOAD"->>'TDD_CLSPRC' AS close,
          "PAYLOAD"->>'ACC_TRDVOL' AS volume, "PAYLOAD"->>'ACC_TRDVAL' AS value,
          "PAYLOAD"->>'LIST_SHRS' AS shares
        FROM "TB_KRX_DATA_ROW"
        WHERE "DATA_CD" IN ('KOSPI_STOCK_DAILY','KOSDAQ_STOCK_DAILY')
          AND "BASE_DT" BETWEEN :from AND :to
          AND (:code='' OR "PAYLOAD"->>'ISU_CD'=:code)
          AND (:market='' OR "DATA_CD"=:dataset)
        ORDER BY "BASE_DT", "DATA_CD", "ROW_KEY"
        """)
        .param("from", from)
        .param("to", to)
        .param("code", code == null ? "" : code)
        .param("market", market == null ? "" : market)
        .param("dataset", market + "_STOCK_DAILY")
        .query(
            (rs, row) ->
                new Bar(
                    rs.getObject("BASE_DT", LocalDate.class),
                    rs.getString("DATA_CD").startsWith("KOSPI_") ? "KOSPI" : "KOSDAQ",
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("section"),
                    number(rs.getString("open")),
                    number(rs.getString("high")),
                    number(rs.getString("low")),
                    number(rs.getString("close")),
                    number(rs.getString("volume")),
                    number(rs.getString("value")),
                    number(rs.getString("shares"))))
        .list();
  }

  static BigDecimal number(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      return new BigDecimal(raw.replace(",", "").trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
