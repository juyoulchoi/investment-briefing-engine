ALTER TABLE "TB_REG_BUY"
    DROP CONSTRAINT IF EXISTS "CK_BUY_SET_06";

UPDATE "TB_REG_BUY"
   SET "WEEK_DAY" = NULL,
       "MONTH_DAY" = NULL,
       "APPLIED_DAY_NOS" = NULL,
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "BUY_STS" = 'STOPPED'
    OR "USER_PAUSE_YN" = 'Y';

ALTER TABLE "TB_REG_BUY"
    ADD CONSTRAINT "CK_BUY_SET_06" CHECK (
        (
            ("BUY_STS" = 'STOPPED' OR "USER_PAUSE_YN" = 'Y')
            AND "WEEK_DAY" IS NULL
            AND "MONTH_DAY" IS NULL
            AND "APPLIED_DAY_NOS" IS NULL
        )
        OR
        (
            "BUY_STS" <> 'STOPPED'
            AND "USER_PAUSE_YN" <> 'Y'
            AND (
                ("CYCLE_TP" = 'WEEKLY' AND "WEEK_DAY" IS NOT NULL AND "MONTH_DAY" IS NULL)
                OR
                ("CYCLE_TP" = 'MONTHLY' AND "WEEK_DAY" IS NULL AND "MONTH_DAY" IS NOT NULL)
                OR
                ("CYCLE_TP" IN ('DAILY', 'MANUAL', 'PAUSED') AND "WEEK_DAY" IS NULL AND "MONTH_DAY" IS NULL)
            )
        )
    );

COMMENT ON CONSTRAINT "CK_BUY_SET_06" ON "TB_REG_BUY" IS
    '매수 중지 또는 사용자 일시정지이면 매수요일과 매수일을 모두 비우고, 그 외에는 적용주기에 맞는 매수일을 사용한다.';
