package com.nanum.investment.marketdata.infrastructure;

import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class KrxDerivativeIndexCollector {
  private final JdbcClient jdbc;

  public KrxDerivativeIndexCollector(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public int normalize(LocalDate date) {
    return jdbc.sql("""
        INSERT INTO "TB_KRX_DRV_IDX_DAY"
          ("BASE_DT", "IDX_CLSS", "IDX_NM", "CLS_IDX", "CHG_IDX", "CHG_RT",
           "OPEN_IDX", "HIGH_IDX", "LOW_IDX", "TRD_VOL", "TRD_AMT", "RAW_PAYLOAD")
        SELECT "BASE_DT", "PAYLOAD"->>'IDX_CLSS', "PAYLOAD"->>'IDX_NM',
          NULLIF(replace("PAYLOAD"->>'CLSPRC_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'CMPPREVDD_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'FLUC_RT', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'OPNPRC_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'HGPRC_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'LWPRC_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'ACC_TRDVOL', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'ACC_TRDVAL', ',', ''), '')::numeric, "PAYLOAD"
        FROM "TB_KRX_DATA_ROW"
        WHERE "DATA_CD" = 'DERIVATIVE_INDEX_DAILY' AND "BASE_DT" = :date
        ON CONFLICT ("BASE_DT", "IDX_CLSS", "IDX_NM") DO UPDATE SET
          "CLS_IDX"=EXCLUDED."CLS_IDX", "CHG_IDX"=EXCLUDED."CHG_IDX", "CHG_RT"=EXCLUDED."CHG_RT",
          "OPEN_IDX"=EXCLUDED."OPEN_IDX", "HIGH_IDX"=EXCLUDED."HIGH_IDX", "LOW_IDX"=EXCLUDED."LOW_IDX",
          "TRD_VOL"=EXCLUDED."TRD_VOL", "TRD_AMT"=EXCLUDED."TRD_AMT",
          "RAW_PAYLOAD"=EXCLUDED."RAW_PAYLOAD", "MOD_DTTM"=CURRENT_TIMESTAMP
        """).param("date", date).update();
  }
}
