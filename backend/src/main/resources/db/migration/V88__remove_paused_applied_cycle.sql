UPDATE "TB_REG_BUY"
   SET "CYCLE_TP" = CASE "BUY_CYCLE"
           WHEN 'DAILY' THEN 'DAILY'
           WHEN 'WEEKLY' THEN 'WEEKLY'
           ELSE 'MONTHLY'
       END,
       "WEEK_DAY" = CASE
           WHEN "BUY_CYCLE" = 'WEEKLY'
               THEN COALESCE(NULLIF("BUY_DAY_CD", ''), 'MON')
           ELSE NULL
       END,
       "APPLIED_DAY_NOS" = CASE
           WHEN "BUY_CYCLE" IN ('MONTHLY', 'CUSTOM')
               THEN COALESCE(
                   NULLIF("APPLIED_DAY_NOS", ''),
                   NULLIF("BUY_DAY_NOS", ''),
                   "MONTH_DAY"::VARCHAR,
                   '15'
               )
           ELSE NULL
       END,
       "MONTH_DAY" = CASE
           WHEN "BUY_CYCLE" IN ('MONTHLY', 'CUSTOM')
               THEN SPLIT_PART(
                   COALESCE(
                       NULLIF("APPLIED_DAY_NOS", ''),
                       NULLIF("BUY_DAY_NOS", ''),
                       "MONTH_DAY"::VARCHAR,
                       '15'
                   ),
                   ',',
                   1
               )::INTEGER
           ELSE NULL
       END,
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "CYCLE_TP" IN ('PAUSED', 'MANUAL');

ALTER TABLE "TB_REG_BUY"
    ADD CONSTRAINT "CK_TB_REG_BUY_APPLIED_CYCLE" CHECK (
        "CYCLE_TP" IN ('DAILY', 'WEEKLY', 'MONTHLY')
    );

DO $$
DECLARE
    V_INVALID_CYCLE_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_INVALID_CYCLE_COUNT
      FROM "TB_REG_BUY"
     WHERE "CYCLE_TP" NOT IN ('DAILY', 'WEEKLY', 'MONTHLY');

    IF V_INVALID_CYCLE_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Applied cycle normalization failed: invalid cycles %',
            V_INVALID_CYCLE_COUNT;
    END IF;
END $$;
