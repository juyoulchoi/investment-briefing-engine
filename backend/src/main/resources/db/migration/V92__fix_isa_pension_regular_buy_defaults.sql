UPDATE "TB_REG_BUY"
   SET "BUY_BASIS" = 'QUANTITY',
       "MIN_BUY_AMT" = 0,
       "BASE_QTY" = 1,
       "BUY_CYCLE" = 'WEEKLY',
       "BUY_DAY_CD" = 'TUE',
       "BUY_DAY_NO" = NULL,
       "BUY_DAY_NOS" = NULL,
       "BASE_CYCLE_TP" = 'WEEKLY',
       "BASE_WEEK_DAY" = 'TUE',
       "BASE_MONTH_DAY" = NULL,
       "BASE_AMT" = NULL,
       "MOD_DT" = CURRENT_TIMESTAMP,
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
 WHERE "ACCT_TP" IN ('ISA', 'PENSION')
   AND "DEL_YN" = 'N';

DO $$
DECLARE
    V_INVALID_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_INVALID_COUNT
      FROM "TB_REG_BUY"
     WHERE "ACCT_TP" IN ('ISA', 'PENSION')
       AND "DEL_YN" = 'N'
       AND (
           "BUY_BASIS" <> 'QUANTITY'
           OR "BASE_QTY" <> 1
           OR "BUY_CYCLE" <> 'WEEKLY'
           OR "BUY_DAY_CD" <> 'TUE'
           OR "BUY_DAY_NO" IS NOT NULL
           OR "BUY_DAY_NOS" IS NOT NULL
       );

    IF V_INVALID_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Fixed ISA/pension regular-buy defaults failed: invalid rows %',
            V_INVALID_COUNT;
    END IF;
END $$;
