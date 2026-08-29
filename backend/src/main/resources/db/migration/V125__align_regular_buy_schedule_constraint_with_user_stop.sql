ALTER TABLE "TB_REG_BUY"
    DROP CONSTRAINT IF EXISTS "CK_BUY_SET_06";

ALTER TABLE "TB_REG_BUY"
    ADD CONSTRAINT "CK_BUY_SET_06" CHECK (
        (
            "BUY_STS" = 'STOPPED'
            AND "USER_PAUSE_YN" = 'Y'
            AND "WEEK_DAY" IS NULL
            AND "MONTH_DAY" IS NULL
            AND "APPLIED_DAY_NOS" IS NULL
        )
        OR
        (
            NOT ("BUY_STS" = 'STOPPED' AND "USER_PAUSE_YN" = 'Y')
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
    '적용주기에 맞는 매수일 필수값. 단, 사용자 중지(STOPPED/Y)는 매수요일과 매수일을 모두 비운다.';
