UPDATE "TB_REG_BUY"
   SET "MIN_BUY_AMT" = 10000,
       "BASE_QTY" = 1,
       "BASE_AMT" = 10000,
       "MOD_DT" = CURRENT_TIMESTAMP,
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
 WHERE "DEL_YN" = 'N';

DO $$
DECLARE
    V_INVALID_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_INVALID_COUNT
      FROM "TB_REG_BUY"
     WHERE "DEL_YN" = 'N'
       AND (
           "MIN_BUY_AMT" <> 10000
           OR "BASE_QTY" <> 1
       );

    IF V_INVALID_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Fixed regular-buy basis values failed: invalid rows %',
            V_INVALID_COUNT;
    END IF;
END $$;
