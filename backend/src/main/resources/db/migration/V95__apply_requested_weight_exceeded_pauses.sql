CREATE TEMPORARY TABLE "TMP_REQUESTED_WEIGHT_EXCEEDED_PAUSE" (
    "ACCT_TP" VARCHAR(20) NOT NULL,
    "MKT_CD" VARCHAR(10) NOT NULL,
    "STK_CD" VARCHAR(30) NOT NULL,
    PRIMARY KEY ("ACCT_TP", "MKT_CD", "STK_CD")
);

INSERT INTO "TMP_REQUESTED_WEIGHT_EXCEEDED_PAUSE" ("ACCT_TP", "MKT_CD", "STK_CD")
VALUES
    ('ISA', 'KO', '471990'),
    ('ISA', 'KO', '160580'),
    ('ISA', 'KO', '464310'),
    ('ISA', 'KO', '0183J0'),
    ('ISA', 'KO', '305080'),
    ('DOMESTIC', 'KO', '007340'),
    ('DOMESTIC', 'KO', '010120'),
    ('DOMESTIC', 'KO', '000660'),
    ('DOMESTIC', 'KO', '028050'),
    ('DOMESTIC', 'KO', '032820'),
    ('DOMESTIC', 'KO', '000100'),
    ('DOMESTIC', 'KO', '014680'),
    ('DOMESTIC', 'KO', '298040'),
    ('PENSION', 'KO', '411060'),
    ('PENSION', 'KO', '471990'),
    ('PENSION', 'KO', '117700'),
    ('PENSION', 'KO', '0089D0'),
    ('PENSION', 'KO', '144600'),
    ('PENSION', 'KO', '266420'),
    ('PENSION', 'KO', '161510'),
    ('PENSION', 'KO', '0023A0'),
    ('PENSION', 'KO', '0051G0'),
    ('PENSION', 'KO', '139270'),
    ('PENSION', 'KO', '227550'),
    ('PENSION', 'KO', '160580'),
    ('PENSION', 'KO', '464310'),
    ('PENSION', 'KO', '329200'),
    ('PENSION', 'KO', '0183J0'),
    ('PENSION', 'KO', '305080'),
    ('PENSION', 'KO', '0046A0'),
    ('PENSION', 'KO', '494670'),
    ('OVERSEAS', 'US', 'SMH'),
    ('OVERSEAS', 'US', 'XLI'),
    ('OVERSEAS', 'US', 'XLV'),
    ('OVERSEAS', 'US', 'MSFT'),
    ('OVERSEAS', 'US', 'BAC'),
    ('OVERSEAS', 'US', 'BRK.B'),
    ('OVERSEAS', 'US', 'AVGO'),
    ('OVERSEAS', 'US', 'VST'),
    ('OVERSEAS', 'US', 'V'),
    ('OVERSEAS', 'US', 'SPCX'),
    ('OVERSEAS', 'US', 'AMZN'),
    ('OVERSEAS', 'US', 'IONQ'),
    ('OVERSEAS', 'US', 'ABBV'),
    ('OVERSEAS', 'US', 'AAPL'),
    ('OVERSEAS', 'US', 'INTC'),
    ('OVERSEAS', 'US', 'LLY'),
    ('OVERSEAS', 'US', 'JPM'),
    ('OVERSEAS', 'US', 'CAT'),
    ('OVERSEAS', 'US', 'COST');

DO $$
DECLARE
    V_TARGET_COUNT INTEGER;
    V_MATCHED_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_TARGET_COUNT
      FROM "TMP_REQUESTED_WEIGHT_EXCEEDED_PAUSE";

    SELECT COUNT(*) INTO V_MATCHED_COUNT
      FROM "TMP_REQUESTED_WEIGHT_EXCEEDED_PAUSE" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = t."ACCT_TP"
       AND (r."MKT_CD" = t."MKT_CD" OR r."MKT_CD" IS NULL)
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N';

    IF V_TARGET_COUNT <> 50 OR V_MATCHED_COUNT <> V_TARGET_COUNT THEN
        RAISE EXCEPTION
            'Requested weight-exceeded pause target mismatch: targets %, matched %',
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
  FROM "TMP_REQUESTED_WEIGHT_EXCEEDED_PAUSE" t
 WHERE r."ACCT_TP" = t."ACCT_TP"
   AND (r."MKT_CD" = t."MKT_CD" OR r."MKT_CD" IS NULL)
   AND r."STK_CD" = t."STK_CD"
   AND r."DEL_YN" = 'N';

DO $$
DECLARE
    V_MISMATCH_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_MISMATCH_COUNT
      FROM "TMP_REQUESTED_WEIGHT_EXCEEDED_PAUSE" t
      JOIN "TB_REG_BUY" r
        ON r."ACCT_TP" = t."ACCT_TP"
       AND (r."MKT_CD" = t."MKT_CD" OR r."MKT_CD" IS NULL)
       AND r."STK_CD" = t."STK_CD"
       AND r."DEL_YN" = 'N'
     WHERE r."BUY_STS" <> 'ACTIVE'
        OR r."ACTV_YN" <> 'Y'
        OR r."USER_PAUSE_YN" <> 'Y'
        OR r."PAUSE_RSN" <> '비중 초과';

    IF V_MISMATCH_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Requested weight-exceeded pause verification failed: mismatches %',
            V_MISMATCH_COUNT;
    END IF;
END $$;
