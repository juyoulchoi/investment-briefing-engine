package com.nanum.investment.marketdata.infrastructure;

import com.nanum.investment.marketdata.domain.KrxDataset;
import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class KrxBondTradingDailyCollector {
  private final JdbcClient jdbc;
  public KrxBondTradingDailyCollector(JdbcClient jdbc) { this.jdbc = jdbc; }

  public int normalize(KrxDataset dataset, LocalDate date) {
    String market = dataset == KrxDataset.GOVERNMENT_BOND_DAILY ? "KTS" :
        dataset == KrxDataset.SMALL_BOND_DAILY ? "SMALL" : "GENERAL";
    jdbc.sql("""
        INSERT INTO "TB_BOND" ("ISU_CD","ISU_NM","BOND_TP","MKT_CD","ISSUER_NM","ISSUE_DT",
          "MATURITY_DT","COUPON_RT","PAR_VAL","DATA_SRC_CD","USE_YN","DEL_YN")
        SELECT DISTINCT COALESCE(NULLIF("PAYLOAD"->>'ISU_CD',''),"PAYLOAD"->>'ISU_SRT_CD'),
          COALESCE(NULLIF("PAYLOAD"->>'ISU_NM',''),"PAYLOAD"->>'ISU_ABBRV'),
          COALESCE(NULLIF("PAYLOAD"->>'BOND_TP_NM',''),:market),:market,
          COALESCE("PAYLOAD"->>'ISUR_NM',"PAYLOAD"->>'ISSUER_NM'),
          CASE WHEN "PAYLOAD"->>'ISSU_DD' ~ '^[0-9]{8}$' THEN to_date("PAYLOAD"->>'ISSU_DD','YYYYMMDD') END,
          CASE WHEN COALESCE("PAYLOAD"->>'MTRT_DD',"PAYLOAD"->>'EXPIRATION_DT') ~ '^[0-9]{8}$'
            THEN to_date(COALESCE("PAYLOAD"->>'MTRT_DD',"PAYLOAD"->>'EXPIRATION_DT'),'YYYYMMDD') END,
          NULLIF(replace(COALESCE("PAYLOAD"->>'COUPON_RT',"PAYLOAD"->>'INT_RT'),',',''),'')::numeric,
          NULLIF(replace(COALESCE("PAYLOAD"->>'PARVAL',"PAYLOAD"->>'PAR_VAL'),',',''),'')::numeric,
          'KRX','Y','N' FROM "TB_KRX_DATA_ROW" WHERE "DATA_CD"=:dataset AND "BASE_DT"=:date
          AND COALESCE(NULLIF("PAYLOAD"->>'ISU_CD',''),"PAYLOAD"->>'ISU_SRT_CD') IS NOT NULL
        ON CONFLICT ("ISU_CD") DO UPDATE SET "ISU_NM"=EXCLUDED."ISU_NM", "BOND_TP"=EXCLUDED."BOND_TP",
          "MKT_CD"=EXCLUDED."MKT_CD","ISSUER_NM"=EXCLUDED."ISSUER_NM","MATURITY_DT"=EXCLUDED."MATURITY_DT",
          "USE_YN"='Y',"DEL_YN"='N',"UPD_DTTM"=CURRENT_TIMESTAMP
        """).param("market",market).param("dataset",dataset.name()).param("date",date).update();
    return jdbc.sql("""
        INSERT INTO "TB_BOND_TRD_DAY" ("BOND_ID","TRADE_DT","OPEN_PRC","HIGH_PRC","LOW_PRC","CLS_PRC",
          "CLS_YLD_RT","CHG_AMT","CHG_RT","TRD_VOL","TURNOVER_AMT","DATA_SRC_CD","DATA_STS")
        SELECT b."BOND_ID",r."BASE_DT",NULLIF(replace(r."PAYLOAD"->>'TDD_OPNPRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'TDD_HGPRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'TDD_LWPRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'TDD_CLSPRC',',',''),'')::numeric,
          NULLIF(replace(COALESCE(r."PAYLOAD"->>'CLSPRC_YD',r."PAYLOAD"->>'YLD_RT'),',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'CMPPREVDD_PRC',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'FLUC_RT',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'ACC_TRDVOL',',',''),'')::numeric,
          NULLIF(replace(r."PAYLOAD"->>'ACC_TRDVAL',',',''),'')::numeric,'KRX','FRESH'
        FROM "TB_KRX_DATA_ROW" r JOIN "TB_BOND" b
          ON b."ISU_CD"=COALESCE(NULLIF(r."PAYLOAD"->>'ISU_CD',''),r."PAYLOAD"->>'ISU_SRT_CD')
        WHERE r."DATA_CD"=:dataset AND r."BASE_DT"=:date
        ON CONFLICT ("BOND_ID","TRADE_DT") DO UPDATE SET "OPEN_PRC"=EXCLUDED."OPEN_PRC",
          "HIGH_PRC"=EXCLUDED."HIGH_PRC","LOW_PRC"=EXCLUDED."LOW_PRC","CLS_PRC"=EXCLUDED."CLS_PRC",
          "CLS_YLD_RT"=EXCLUDED."CLS_YLD_RT","CHG_AMT"=EXCLUDED."CHG_AMT","CHG_RT"=EXCLUDED."CHG_RT",
          "TRD_VOL"=EXCLUDED."TRD_VOL","TURNOVER_AMT"=EXCLUDED."TURNOVER_AMT",
          "DATA_STS"='FRESH',"COLLECT_DTTM"=CURRENT_TIMESTAMP
        """).param("dataset",dataset.name()).param("date",date).update();
  }
}
