CREATE TEMPORARY TABLE "TMP_OVERSEAS_WEEKLY_BUY" (
    "STK_CD" VARCHAR(30) PRIMARY KEY,
    "CYCLE_TP" VARCHAR(20),
    "WEEK_DAY" VARCHAR(50),
    "APPLIED_DAY_NOS" VARCHAR(100),
    "AMT" NUMERIC(20, 4),
    "BUY_STS" VARCHAR(20) NOT NULL,
    "ACTV_YN" CHAR(1) NOT NULL,
    "USER_PAUSE_YN" CHAR(1) NOT NULL,
    "PAUSE_RSN" VARCHAR(500)
);

INSERT INTO "TMP_OVERSEAS_WEEKLY_BUY"
    ("STK_CD", "CYCLE_TP", "WEEK_DAY", "APPLIED_DAY_NOS", "AMT",
     "BUY_STS", "ACTV_YN", "USER_PAUSE_YN", "PAUSE_RSN")
VALUES
    ('BOTZ',  NULL,      NULL,      NULL,   NULL,  'STOPPED', 'N', 'N', '비중 초과'),
    ('GEV',   'WEEKLY',  'TUE,FRI', NULL,   2000,  'ACTIVE',  'Y', 'N', NULL),
    ('HYDR',  NULL,      NULL,      NULL,   NULL,  'STOPPED', 'N', 'N', '비중 초과'),
    ('QQQ',   'DAILY',   NULL,      NULL,   4000,  'ACTIVE',  'Y', 'Y', '비중 초과'),
    ('SCHD',  'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('SMH',   'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('SPY',   'DAILY',   NULL,      NULL,   4000,  'ACTIVE',  'Y', 'Y', '비중 초과'),
    ('VIG',   'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('XLF',   NULL,      NULL,      NULL,   NULL,  'STOPPED', 'N', 'N', '비중 초과'),
    ('XLI',   'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('XLV',   'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('MSFT',  'DAILY',   NULL,      NULL,   2000,  'ACTIVE',  'Y', 'N', NULL),
    ('BAC',   NULL,      NULL,      NULL,   NULL,  'STOPPED', 'N', 'N', '투자 전략 변경'),
    ('BRK.B', 'MONTHLY', NULL,      '15',   20000, 'ACTIVE',  'Y', 'N', NULL),
    ('VRT',   'DAILY',   NULL,      NULL,   1500,  'ACTIVE',  'Y', 'Y', '비중 초과'),
    ('AVGO',  'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('VST',   'WEEKLY',  'TUE',     NULL,   7000,  'ACTIVE',  'Y', 'N', NULL),
    ('V',     'MONTHLY', NULL,      '15',   10000, 'ACTIVE',  'Y', 'N', NULL),
    ('SPCX',  'WEEKLY',  'TUE',     NULL,   3000,  'ACTIVE',  'Y', 'N', NULL),
    ('ANET',  NULL,      NULL,      NULL,   NULL,  'STOPPED', 'N', 'N', '비중 초과'),
    ('AMZN',  'DAILY',   NULL,      NULL,   1500,  'ACTIVE',  'Y', 'N', NULL),
    ('IONQ',  NULL,      NULL,      NULL,   NULL,  'STOPPED', 'N', 'N', '매수 조건 미충족'),
    ('GOOGL', 'WEEKLY',  'TUE,FRI', NULL,   2000,  'ACTIVE',  'Y', 'N', NULL),
    ('ABBV',  'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('AAPL',  'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('NVDA',  'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('WMT',   'DAILY',   NULL,      NULL,   2000,  'ACTIVE',  'Y', 'Y', '비중 초과'),
    ('INTC',  'WEEKLY',  'TUE',     NULL,   2000,  'ACTIVE',  'Y', 'N', NULL),
    ('LLY',   'WEEKLY',  'TUE',     NULL,   7000,  'ACTIVE',  'Y', 'N', NULL),
    ('JPM',   'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('JNJ',   'DAILY',   NULL,      NULL,   2000,  'ACTIVE',  'Y', 'Y', '비중 초과'),
    ('CAT',   'WEEKLY',  'TUE',     NULL,   10000, 'ACTIVE',  'Y', 'N', NULL),
    ('COST',  'MONTHLY', NULL,      '15',   10000, 'ACTIVE',  'Y', 'N', NULL),
    ('CEG',   'DAILY',   NULL,      NULL,   1000,  'ACTIVE',  'Y', 'N', NULL),
    ('PLTR',  'MONTHLY', NULL,      '1,15', 7000,  'ACTIVE',  'Y', 'N', NULL),
    ('PLUG',  NULL,      NULL,      NULL,   NULL,  'STOPPED', 'N', 'N', '비중 초과');

DO $$
DECLARE
    V_TARGET_COUNT INTEGER;
    V_MATCHED_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_TARGET_COUNT FROM "TMP_OVERSEAS_WEEKLY_BUY";
    SELECT COUNT(*) INTO V_MATCHED_COUNT
      FROM "TMP_OVERSEAS_WEEKLY_BUY" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = 'OVERSEAS'
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N';

    IF V_TARGET_COUNT <> 36 OR V_MATCHED_COUNT <> V_TARGET_COUNT THEN
        RAISE EXCEPTION
            'Overseas weekly-buy target mismatch: targets %, matched %',
            V_TARGET_COUNT, V_MATCHED_COUNT;
    END IF;
END $$;

UPDATE "TB_REG_BUY" r
   SET "CYCLE_TP" = COALESCE(t."CYCLE_TP", r."CYCLE_TP"),
       "WEEK_DAY" = CASE
           WHEN t."CYCLE_TP" IS NULL THEN r."WEEK_DAY"
           ELSE t."WEEK_DAY"
       END,
       "APPLIED_DAY_NOS" = CASE
           WHEN t."CYCLE_TP" IS NULL THEN r."APPLIED_DAY_NOS"
           ELSE t."APPLIED_DAY_NOS"
       END,
       "MONTH_DAY" = CASE
           WHEN t."CYCLE_TP" IS NULL THEN r."MONTH_DAY"
           WHEN t."APPLIED_DAY_NOS" IS NULL THEN NULL
           ELSE CAST(SPLIT_PART(t."APPLIED_DAY_NOS", ',', 1) AS INTEGER)
       END,
       "AMT" = COALESCE(t."AMT", r."AMT"),
       "BUY_STS" = t."BUY_STS",
       "ACTV_YN" = t."ACTV_YN",
       "USER_PAUSE_YN" = t."USER_PAUSE_YN",
       "PAUSE_RSN" = t."PAUSE_RSN",
       "MOD_DT" = CURRENT_TIMESTAMP,
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
  FROM "TMP_OVERSEAS_WEEKLY_BUY" t
 WHERE r."ACCT_TP" = 'OVERSEAS'
   AND r."STK_CD" = t."STK_CD"
   AND r."DEL_YN" = 'N';

DO $$
DECLARE
    V_MISMATCH_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_MISMATCH_COUNT
      FROM "TMP_OVERSEAS_WEEKLY_BUY" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = 'OVERSEAS'
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N'
     WHERE (t."CYCLE_TP" IS NOT NULL AND r."CYCLE_TP" IS DISTINCT FROM t."CYCLE_TP")
        OR (t."CYCLE_TP" IS NOT NULL AND r."WEEK_DAY" IS DISTINCT FROM t."WEEK_DAY")
        OR (t."CYCLE_TP" IS NOT NULL AND r."APPLIED_DAY_NOS" IS DISTINCT FROM t."APPLIED_DAY_NOS")
        OR (t."AMT" IS NOT NULL AND r."AMT" IS DISTINCT FROM t."AMT")
        OR r."BUY_STS" IS DISTINCT FROM t."BUY_STS"
        OR r."ACTV_YN" IS DISTINCT FROM t."ACTV_YN"
        OR r."USER_PAUSE_YN" IS DISTINCT FROM t."USER_PAUSE_YN"
        OR r."PAUSE_RSN" IS DISTINCT FROM t."PAUSE_RSN";

    IF V_MISMATCH_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Overseas weekly-buy verification failed: mismatches %',
            V_MISMATCH_COUNT;
    END IF;
END $$;
