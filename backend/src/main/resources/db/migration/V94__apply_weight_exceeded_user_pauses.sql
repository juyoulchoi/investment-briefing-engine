CREATE TEMPORARY TABLE "TMP_WEIGHT_EXCEEDED_PAUSE" (
    "ACCT_TP" VARCHAR(20) NOT NULL,
    "STK_CD" VARCHAR(30) NOT NULL,
    PRIMARY KEY ("ACCT_TP", "STK_CD")
);

INSERT INTO "TMP_WEIGHT_EXCEEDED_PAUSE" ("ACCT_TP", "STK_CD")
VALUES
    ('ISA', '069500'),
    ('ISA', '305720'),
    ('ISA', '379800'),
    ('ISA', '161510'),
    ('ISA', '0023A0'),
    ('ISA', '458730'),
    ('ISA', '0046A0'),
    ('ISA', '381180'),
    ('ISA', '466940'),
    ('ISA', '302190'),
    ('DOMESTIC', '105560'),
    ('DOMESTIC', '034020'),
    ('DOMESTIC', '058470'),
    ('DOMESTIC', '005930'),
    ('DOMESTIC', '000720'),
    ('PENSION', '069500'),
    ('PENSION', '455890'),
    ('PENSION', '463250'),
    ('PENSION', '360750'),
    ('PENSION', '458730'),
    ('PENSION', '241180'),
    ('PENSION', '302190'),
    ('OVERSEAS', 'BOTZ'),
    ('OVERSEAS', 'HYDR'),
    ('OVERSEAS', 'QQQ'),
    ('OVERSEAS', 'SPY'),
    ('OVERSEAS', 'XLF'),
    ('OVERSEAS', 'VRT'),
    ('OVERSEAS', 'ANET'),
    ('OVERSEAS', 'WMT'),
    ('OVERSEAS', 'JNJ'),
    ('OVERSEAS', 'PLUG');

DO $$
DECLARE
    V_TARGET_COUNT INTEGER;
    V_MATCHED_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_TARGET_COUNT
      FROM "TMP_WEIGHT_EXCEEDED_PAUSE";

    SELECT COUNT(*) INTO V_MATCHED_COUNT
      FROM "TMP_WEIGHT_EXCEEDED_PAUSE" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = t."ACCT_TP"
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N';

    IF V_TARGET_COUNT <> 32 OR V_MATCHED_COUNT <> V_TARGET_COUNT THEN
        RAISE EXCEPTION
            'Weight-exceeded pause target mismatch: targets %, matched %',
            V_TARGET_COUNT,
            V_MATCHED_COUNT;
    END IF;
END $$;

UPDATE "TB_REG_BUY" r
   SET "BUY_STS" = 'ACTIVE',
       "ACTV_YN" = 'Y',
       "USER_PAUSE_YN" = 'Y',
       "PAUSE_RSN" = '비중 초과',
       "MOD_DT" = CURRENT_TIMESTAMP,
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
  FROM "TMP_WEIGHT_EXCEEDED_PAUSE" t
 WHERE r."ACCT_TP" = t."ACCT_TP"
   AND r."STK_CD" = t."STK_CD"
   AND r."DEL_YN" = 'N';

DO $$
DECLARE
    V_MISMATCH_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_MISMATCH_COUNT
      FROM "TMP_WEIGHT_EXCEEDED_PAUSE" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = t."ACCT_TP"
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N'
     WHERE r."BUY_STS" <> 'ACTIVE'
        OR r."ACTV_YN" <> 'Y'
        OR r."USER_PAUSE_YN" <> 'Y'
        OR r."PAUSE_RSN" <> '비중 초과';

    IF V_MISMATCH_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Weight-exceeded pause update verification failed: mismatches %',
            V_MISMATCH_COUNT;
    END IF;
END $$;
