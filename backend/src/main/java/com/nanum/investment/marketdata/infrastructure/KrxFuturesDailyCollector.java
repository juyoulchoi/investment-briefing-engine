package com.nanum.investment.marketdata.infrastructure;

import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class KrxFuturesDailyCollector {
  private final JdbcClient jdbc;

  public KrxFuturesDailyCollector(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public int normalize(LocalDate date) {
    return jdbc.sql("""
        INSERT INTO "TB_KRX_FUT_DAY"
          ("BASE_DT", "ISU_CD", "ISU_NM", "EXPIRATION_CD", "CLS_PRC", "OPEN_PRC",
           "HIGH_PRC", "LOW_PRC", "TRD_VOL", "TRD_AMT", "OPEN_INT", "RAW_PAYLOAD")
        SELECT "BASE_DT", COALESCE(NULLIF("PAYLOAD"->>'ISU_CD',''), "PAYLOAD"->>'ISU_SRT_CD'),
          "PAYLOAD"->>'ISU_NM',
          COALESCE(NULLIF("PAYLOAD"->>'EXPIRATION_CD',''),
                   substring("PAYLOAD"->>'ISU_NM' from '([0-9]{6})')),
          NULLIF(replace("PAYLOAD"->>'TDD_CLSPRC', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'TDD_OPNPRC', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'TDD_HGPRC', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'TDD_LWPRC', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'ACC_TRDVOL', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'ACC_TRDVAL', ',', ''), '')::numeric,
          COALESCE(NULLIF(replace("PAYLOAD"->>'OPNINT_QTY', ',', ''), '')::numeric,
                   NULLIF(replace("PAYLOAD"->>'OPEN_INT', ',', ''), '')::numeric), "PAYLOAD"
        FROM "TB_KRX_DATA_ROW"
        WHERE "DATA_CD" = 'FUTURES_DAILY' AND "BASE_DT" = :date
        ON CONFLICT ("BASE_DT", "ISU_CD") DO UPDATE SET
          "ISU_NM"=EXCLUDED."ISU_NM", "EXPIRATION_CD"=EXCLUDED."EXPIRATION_CD",
          "CLS_PRC"=EXCLUDED."CLS_PRC", "OPEN_PRC"=EXCLUDED."OPEN_PRC",
          "HIGH_PRC"=EXCLUDED."HIGH_PRC", "LOW_PRC"=EXCLUDED."LOW_PRC",
          "TRD_VOL"=EXCLUDED."TRD_VOL", "TRD_AMT"=EXCLUDED."TRD_AMT", "OPEN_INT"=EXCLUDED."OPEN_INT",
          "RAW_PAYLOAD"=EXCLUDED."RAW_PAYLOAD", "MOD_DTTM"=CURRENT_TIMESTAMP
        """).param("date", date).update();
  }
}
