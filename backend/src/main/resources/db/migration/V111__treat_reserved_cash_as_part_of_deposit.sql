-- 기존 CASH_AMT는 대기 현금을 제외한 일반 현금으로 사용되었다.
-- 새 계약에서는 CASH_AMT가 전체 예수금이고 RSV_CASH_AMT는 그 안의 용도 배정액이다.
UPDATE "TB_ACCT"
   SET "CASH_AMT" = COALESCE("CASH_AMT", 0) + COALESCE("RSV_CASH_AMT", 0),
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'FLYWAY_V111';

ALTER TABLE "TB_ACCT"
    ADD CONSTRAINT "CK_TB_ACCT_07"
    CHECK ("RSV_CASH_AMT" <= "CASH_AMT");

COMMENT ON COLUMN "TB_ACCT"."CASH_AMT" IS '전체 예수금(추가매수 대기현금 포함)';
COMMENT ON COLUMN "TB_ACCT"."RSV_CASH_AMT" IS '예수금 중 추가매수에 배정한 대기 현금';
