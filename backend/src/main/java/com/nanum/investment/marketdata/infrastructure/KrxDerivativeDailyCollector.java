package com.nanum.investment.marketdata.infrastructure;

import com.nanum.investment.marketdata.domain.KrxDataset;
import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class KrxDerivativeDailyCollector {
  private final JdbcClient jdbc;
  public KrxDerivativeDailyCollector(JdbcClient jdbc) { this.jdbc = jdbc; }

  public int normalize(KrxDataset dataset, LocalDate date) {
    String type = dataset.name().contains("OPTIONS") ? "OPTION" : "FUTURE";
    String market = dataset.name().contains("KOSDAQ") ? "KOSDAQ" :
        dataset.name().contains("KOSPI") ? "KOSPI" : "GENERAL";
    jdbc.sql("""
        INSERT INTO "TB_DERIV" ("ISU_CD","ISU_NM","DERIV_TP","MKT_CD","UNDERLYING_CD",
          "UNDERLYING_NM","EXPIRATION_CD","STRIKE_PRC","CALL_PUT_TP","DATA_SRC_CD","USE_YN","DEL_YN")
        SELECT DISTINCT COALESCE(NULLIF("PAYLOAD"->>'ISU_CD',''),"PAYLOAD"->>'ISU_SRT_CD'),
          COALESCE(NULLIF("PAYLOAD"->>'ISU_NM',''),"PAYLOAD"->>'ISU_ABBRV'),:type,:market,
          COALESCE("PAYLOAD"->>'ULY_ID',"PAYLOAD"->>'UNDERLYING_CD'),
          COALESCE("PAYLOAD"->>'ULY_NM',"PAYLOAD"->>'UNDERLYING_NM'),
          COALESCE(NULLIF("PAYLOAD"->>'EXPIRATION_CD',''),substring("PAYLOAD"->>'ISU_NM' from '([0-9]{6})')),
          NULLIF(replace(COALESCE("PAYLOAD"->>'EXER_PRC',"PAYLOAD"->>'STRIKE_PRC'),',',''),'')::numeric,
          COALESCE("PAYLOAD"->>'RGHT_TP_NM',"PAYLOAD"->>'CALL_PUT_TP'),'KRX','Y','N'
        FROM "TB_KRX_DATA_ROW" WHERE "DATA_CD"=:dataset AND "BASE_DT"=:date
          AND COALESCE(NULLIF("PAYLOAD"->>'ISU_CD',''),"PAYLOAD"->>'ISU_SRT_CD') IS NOT NULL
        ON CONFLICT ("ISU_CD") DO UPDATE SET "ISU_NM"=EXCLUDED."ISU_NM",
          "EXPIRATION_CD"=EXCLUDED."EXPIRATION_CD", "UNDERLYING_CD"=EXCLUDED."UNDERLYING_CD",
          "UNDERLYING_NM"=EXCLUDED."UNDERLYING_NM", "USE_YN"='Y', "DEL_YN"='N',
          "UPD_DTTM"=CURRENT_TIMESTAMP
        """).param("type",type).param("market",market).param("dataset",dataset.name()).param("date",date).update();
    return jdbc.sql("""
        INSERT INTO "TB_DERIV_DAY" ("DERIV_ID","TRADE_DT","OPEN_PRC","HIGH_PRC","LOW_PRC","CLS_PRC",
          "SETTLE_PRC","CHG_AMT","CHG_RT","TRD_VOL","TURNOVER_AMT","OPEN_INT","DATA_SRC_CD","DATA_STS")
        SELECT d."DERIV_ID",r."BASE_DT",
          NULLIF(replace(r."PAYLOAD"->>'TDD_OPNPRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'TDD_HGPRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'TDD_LWPRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'TDD_CLSPRC',',',''),'')::numeric,
          NULLIF(replace(COALESCE(r."PAYLOAD"->>'SETL_PRC',r."PAYLOAD"->>'SETTLE_PRC'),',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'CMPPREVDD_PRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'FLUC_RT',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'ACC_TRDVOL',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'ACC_TRDVAL',',',''),'')::numeric,
          NULLIF(replace(COALESCE(r."PAYLOAD"->>'OPNINT_QTY',r."PAYLOAD"->>'OPEN_INT'),',',''),'')::numeric,
          'KRX','FRESH' FROM "TB_KRX_DATA_ROW" r JOIN "TB_DERIV" d
          ON d."ISU_CD"=COALESCE(NULLIF(r."PAYLOAD"->>'ISU_CD',''),r."PAYLOAD"->>'ISU_SRT_CD')
        WHERE r."DATA_CD"=:dataset AND r."BASE_DT"=:date
        ON CONFLICT ("DERIV_ID","TRADE_DT") DO UPDATE SET "OPEN_PRC"=EXCLUDED."OPEN_PRC",
          "HIGH_PRC"=EXCLUDED."HIGH_PRC","LOW_PRC"=EXCLUDED."LOW_PRC","CLS_PRC"=EXCLUDED."CLS_PRC",
          "SETTLE_PRC"=EXCLUDED."SETTLE_PRC","CHG_AMT"=EXCLUDED."CHG_AMT","CHG_RT"=EXCLUDED."CHG_RT",
          "TRD_VOL"=EXCLUDED."TRD_VOL","TURNOVER_AMT"=EXCLUDED."TURNOVER_AMT","OPEN_INT"=EXCLUDED."OPEN_INT",
          "DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP
        """).param("dataset",dataset.name()).param("date",date).update();
  }
}
