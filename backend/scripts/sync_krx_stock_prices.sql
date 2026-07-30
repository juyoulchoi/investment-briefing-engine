\set ON_ERROR_STOP on

\if :{?from_date}
\else
  \set from_date '2026-05-04'
\endif

\if :{?to_date}
\else
  \set to_date '2026-07-28'
\endif

INSERT INTO "TB_PRC_DAY"
    ("STK_ID", "MKT_CD", "STK_CD", "TRADE_DT", "OPEN_PRC", "HIGH_PRC", "LOW_PRC",
     "CLS_PRC", "ADJ_CLS_PRC", "VOL", "PRVDR", "DATA_SRC_CD")
SELECT DISTINCT ON (r."BASE_DT", r."PAYLOAD"->>'ISU_CD')
    s."STK_ID", 'KO',
    r."PAYLOAD"->>'ISU_CD',
    r."BASE_DT",
    NULLIF(replace(r."PAYLOAD"->>'TDD_OPNPRC', ',', ''), '')::numeric,
    NULLIF(replace(r."PAYLOAD"->>'TDD_HGPRC', ',', ''), '')::numeric,
    NULLIF(replace(r."PAYLOAD"->>'TDD_LWPRC', ',', ''), '')::numeric,
    NULLIF(replace(r."PAYLOAD"->>'TDD_CLSPRC', ',', ''), '')::numeric,
    NULLIF(replace(r."PAYLOAD"->>'TDD_CLSPRC', ',', ''), '')::numeric,
    COALESCE(NULLIF(replace(r."PAYLOAD"->>'ACC_TRDVOL', ',', ''), '')::bigint, 0),
    'KRX', 'KRX'
FROM "TB_KRX_DATA_ROW" r
JOIN "TB_STK" s
  ON s."MKT_CD" = 'KO'
 AND s."STK_CD" = r."PAYLOAD"->>'ISU_CD'
WHERE r."DATA_CD" IN ('KOSPI_STOCK_DAILY', 'KOSDAQ_STOCK_DAILY', 'ETF_DAILY')
  AND r."BASE_DT" BETWEEN :'from_date'::date AND :'to_date'::date
  AND NULLIF(r."PAYLOAD"->>'TDD_CLSPRC', '') IS NOT NULL
ORDER BY r."BASE_DT", r."PAYLOAD"->>'ISU_CD'
ON CONFLICT ("MKT_CD", "STK_CD", "TRADE_DT") DO UPDATE
SET "OPEN_PRC" = EXCLUDED."OPEN_PRC",
    "HIGH_PRC" = EXCLUDED."HIGH_PRC",
    "LOW_PRC" = EXCLUDED."LOW_PRC",
    "CLS_PRC" = EXCLUDED."CLS_PRC",
    "ADJ_CLS_PRC" = EXCLUDED."ADJ_CLS_PRC",
    "VOL" = EXCLUDED."VOL",
    "PRVDR" = EXCLUDED."PRVDR",
    "MOD_DT" = CURRENT_TIMESTAMP;




