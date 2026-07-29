INSERT INTO "TB_COM_CD" ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('WEEK_DAY', 'MON', '월요일', '정기 모으기 월요일 실행 코드', 1, 'Y'),
    ('WEEK_DAY', 'TUE', '화요일', '정기 모으기 화요일 실행 코드', 2, 'Y'),
    ('WEEK_DAY', 'WED', '수요일', '정기 모으기 수요일 실행 코드', 3, 'Y'),
    ('WEEK_DAY', 'THU', '목요일', '정기 모으기 목요일 실행 코드', 4, 'Y'),
    ('WEEK_DAY', 'FRI', '금요일', '정기 모으기 금요일 실행 코드', 5, 'Y'),
    ('WEEK_DAY', 'SAT', '토요일', '정기 모으기 토요일 실행 코드', 6, 'Y'),
    ('WEEK_DAY', 'SUN', '일요일', '정기 모으기 일요일 실행 코드', 7, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE SET
    "CD_NM" = EXCLUDED."CD_NM", "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD", "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;

ALTER TABLE "TB_BUY_SET"
    ADD CONSTRAINT "CK_BUY_SET_04" CHECK (
        "WEEK_DAY" IS NULL OR
        "WEEK_DAY" ~ '^(MON|TUE|WED|THU|FRI|SAT|SUN)(,(MON|TUE|WED|THU|FRI|SAT|SUN))*$'),
    ADD CONSTRAINT "CK_BUY_SET_05" CHECK (
        "BASE_WEEK_DAY" IS NULL OR
        "BASE_WEEK_DAY" ~ '^(MON|TUE|WED|THU|FRI|SAT|SUN)(,(MON|TUE|WED|THU|FRI|SAT|SUN))*$'),
    ADD CONSTRAINT "CK_BUY_SET_06" CHECK (
        ("CYCLE_TP" = 'WEEKLY' AND "WEEK_DAY" IS NOT NULL AND "MONTH_DAY" IS NULL)
        OR ("CYCLE_TP" = 'MONTHLY' AND "WEEK_DAY" IS NULL AND "MONTH_DAY" IS NOT NULL)
        OR ("CYCLE_TP" IN ('DAILY', 'MANUAL', 'PAUSED')
            AND "WEEK_DAY" IS NULL AND "MONTH_DAY" IS NULL)),
    ADD CONSTRAINT "CK_BUY_SET_07" CHECK (
        ("BASE_CYCLE_TP" = 'WEEKLY' AND "BASE_WEEK_DAY" IS NOT NULL AND "BASE_MONTH_DAY" IS NULL)
        OR ("BASE_CYCLE_TP" = 'MONTHLY' AND "BASE_WEEK_DAY" IS NULL AND "BASE_MONTH_DAY" IS NOT NULL)
        OR ("BASE_CYCLE_TP" IN ('DAILY', 'MANUAL', 'PAUSED')
            AND "BASE_WEEK_DAY" IS NULL AND "BASE_MONTH_DAY" IS NULL)
        OR "BASE_CYCLE_TP" IS NULL);

COMMENT ON COLUMN "TB_BUY_SET"."WEEK_DAY"
    IS '적용 실행 요일 코드. WEEK_DAY 공통코드를 쉼표로 연결. 예: MON,WED,FRI';
COMMENT ON COLUMN "TB_BUY_SET"."BASE_WEEK_DAY"
    IS '기본 실행 요일 코드. WEEK_DAY 공통코드를 쉼표로 연결. 예: MON,WED,FRI';
