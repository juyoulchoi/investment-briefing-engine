package com.nanum.investment.marketdata.infrastructure;

import com.nanum.investment.marketdata.domain.KrxDataset;
import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class KrxIndexDailyCollector {
  private final JdbcClient jdbc;

  public KrxIndexDailyCollector(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public int normalize(KrxDataset dataset, LocalDate date) {
    jdbc.sql("""
        INSERT INTO "TB_IDX" ("IDX_CD","IDX_NM","IDX_TP","MKT_CD","CNTRY_CD","CURR_CD",
          "DATA_SRC_CD","SRC_SYMBOL","DFLT_YN","USE_YN","DEL_YN","CRT_USR_ID","UPD_USR_ID")
        SELECT DISTINCT CASE
          WHEN upper(replace(coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) IN ('KOSPI','코스피') THEN 'KOSPI'
          WHEN upper(replace(coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) IN ('KOSDAQ','코스닥') THEN 'KOSDAQ'
          WHEN upper(replace(coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) IN ('KOSPI200','코스피200') THEN 'KOSPI200'
          WHEN upper(replace(coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) = 'VKOSPI' THEN 'VKOSPI'
          ELSE 'KRX_' || substr(md5(coalesce("PAYLOAD"->>'IDX_CLSS','BOND') || '|' || coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM')),1,26) END,
          coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM'),'MARKET','KRX','KR','KRW','KRX',
          left(coalesce("PAYLOAD"->>'IDX_CLSS','BOND') || '|' || coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM'),50),'N','Y','N','SYSTEM','SYSTEM'
        FROM "TB_KRX_DATA_ROW" WHERE "DATA_CD"=:dataset AND "BASE_DT"=:date
          AND NULLIF(coalesce("PAYLOAD"->>'IDX_NM',"PAYLOAD"->>'BND_IDX_GRP_NM'),'') IS NOT NULL
        ON CONFLICT ("IDX_CD") DO UPDATE SET "IDX_NM"=EXCLUDED."IDX_NM", "DATA_SRC_CD"='KRX',
          "USE_YN"='Y', "DEL_YN"='N', "UPD_DTTM"=CURRENT_TIMESTAMP, "UPD_USR_ID"='SYSTEM'
        """).param("dataset", dataset.name()).param("date", date).update();
    return jdbc.sql("""
        INSERT INTO "TB_IDX_DAY" ("IDX_CD","TRADE_DT","IND_CD","IND_NM","CLS_VAL","CHG_VAL","CHG_RT",
          "SRC_NM","OPEN_VAL","HIGH_VAL","LOW_VAL","TRD_VOL","TRD_VAL","DATA_SRC_CD","DATA_STS")
        SELECT i."IDX_CD", r."BASE_DT", i."IDX_CD", coalesce(r."PAYLOAD"->>'IDX_NM',r."PAYLOAD"->>'BND_IDX_GRP_NM'),
          NULLIF(replace(coalesce("PAYLOAD"->>'CLSPRC_IDX',"PAYLOAD"->>'TOT_EARNG_IDX'), ',', ''), '')::numeric,
          NULLIF(replace(coalesce("PAYLOAD"->>'CMPPREVDD_IDX',"PAYLOAD"->>'TOT_EARNG_IDX_CMPPREVDD'), ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'FLUC_RT', ',', ''), '')::numeric, 'KRX',
          NULLIF(replace("PAYLOAD"->>'OPNPRC_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'HGPRC_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'LWPRC_IDX', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'ACC_TRDVOL', ',', ''), '')::numeric,
          NULLIF(replace("PAYLOAD"->>'ACC_TRDVAL', ',', ''), '')::numeric, 'KRX','FRESH'
        FROM "TB_KRX_DATA_ROW" r JOIN "TB_IDX" i ON i."IDX_CD" = CASE
          WHEN upper(replace(coalesce(r."PAYLOAD"->>'IDX_NM',r."PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) IN ('KOSPI','코스피') THEN 'KOSPI'
          WHEN upper(replace(coalesce(r."PAYLOAD"->>'IDX_NM',r."PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) IN ('KOSDAQ','코스닥') THEN 'KOSDAQ'
          WHEN upper(replace(coalesce(r."PAYLOAD"->>'IDX_NM',r."PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) IN ('KOSPI200','코스피200') THEN 'KOSPI200'
          WHEN upper(replace(coalesce(r."PAYLOAD"->>'IDX_NM',r."PAYLOAD"->>'BND_IDX_GRP_NM'),' ','')) = 'VKOSPI' THEN 'VKOSPI'
          ELSE 'KRX_' || substr(md5(coalesce(r."PAYLOAD"->>'IDX_CLSS','BOND') || '|' || coalesce(r."PAYLOAD"->>'IDX_NM',r."PAYLOAD"->>'BND_IDX_GRP_NM')),1,26) END
        WHERE r."DATA_CD"=:dataset AND r."BASE_DT"=:date
        ON CONFLICT ("IDX_CD","TRADE_DT") DO UPDATE SET "CLS_VAL"=EXCLUDED."CLS_VAL",
          "CHG_VAL"=EXCLUDED."CHG_VAL", "CHG_RT"=EXCLUDED."CHG_RT", "OPEN_VAL"=EXCLUDED."OPEN_VAL",
          "HIGH_VAL"=EXCLUDED."HIGH_VAL", "LOW_VAL"=EXCLUDED."LOW_VAL", "TRD_VOL"=EXCLUDED."TRD_VOL",
          "TRD_VAL"=EXCLUDED."TRD_VAL", "DATA_SRC_CD"='KRX', "DATA_STS"='FRESH',
          "COLLECT_DTTM"=CURRENT_TIMESTAMP
        """).param("dataset", dataset.name()).param("date", date).update();
  }
}
