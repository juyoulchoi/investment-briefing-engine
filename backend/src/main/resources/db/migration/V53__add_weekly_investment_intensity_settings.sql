ALTER TABLE "TB_STK_SET"
    ADD COLUMN "MIN_INV_AMT" NUMERIC(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN "MAX_INV_AMT" NUMERIC(18, 2) NOT NULL DEFAULT 0,
    ADD COLUMN "MAX_INC_MULT" NUMERIC(4, 2) NOT NULL DEFAULT 3.00;

ALTER TABLE "TB_STK_SET"
    ADD CONSTRAINT "CK_STK_SET_04" CHECK ("MIN_INV_AMT" >= 0 AND "MAX_INV_AMT" >= 0),
    ADD CONSTRAINT "CK_STK_SET_05" CHECK ("MAX_INV_AMT" = 0 OR "MAX_INV_AMT" >= "MIN_INV_AMT"),
    ADD CONSTRAINT "CK_STK_SET_06" CHECK ("MAX_INC_MULT" BETWEEN 0 AND 10);

COMMENT ON COLUMN "TB_STK_SET"."MIN_INV_AMT" IS '사용자가 입력하는 종목별 최소 투자금액';
COMMENT ON COLUMN "TB_STK_SET"."MAX_INV_AMT" IS '사용자가 입력하는 종목별 최대 투자금액. 0이면 배수 한도로 계산';
COMMENT ON COLUMN "TB_STK_SET"."MAX_INC_MULT" IS '사용자가 입력하는 증액 한도 배수';

ALTER TABLE "TB_INV_STK_DEC"
    ADD COLUMN "MAX_BUY_AMT" BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN "MAX_INC_MULT" NUMERIC(4, 2) NOT NULL DEFAULT 3.00,
    ADD COLUMN "ADJ_RSN" TEXT NOT NULL DEFAULT '',
    ADD COLUMN "CASH_PLAN" TEXT NOT NULL DEFAULT '';

COMMENT ON COLUMN "TB_INV_STK_DEC"."MULT" IS '이번 주 권장 투자배수';
COMMENT ON COLUMN "TB_INV_STK_DEC"."RCM_BUY_AMT" IS '이번 주 실제 권장 투자금액';
COMMENT ON COLUMN "TB_INV_STK_DEC"."ADJ_RSN" IS '시장·심리·비중을 반영한 증액 또는 감액 이유';
COMMENT ON COLUMN "TB_INV_STK_DEC"."CASH_PLAN" IS '감액으로 확보되는 현금과 추가매수 계획';

DROP VIEW tb_inv_stk_dec;

CREATE VIEW tb_inv_stk_dec AS
SELECT "ID" AS "id", "INV_DEC_ID" AS "investment_decision_id", "ACCT" AS "account",
       "STK_CD" AS "stock_code", "STK_NM" AS "stock_name", "ACT_SIG" AS "action_signal",
       "MULT" AS "multiplier", "MIN_BUY_AMT" AS "minimum_buy_amount",
       "MAX_BUY_AMT" AS "maximum_buy_amount", "MAX_INC_MULT" AS "maximum_increase_multiplier",
       "RCM_BUY_AMT" AS "recommended_buy_amount", "RSV_CASH" AS "reserved_cash",
       "ADJ_RSN" AS "adjustment_reason", "CASH_PLAN" AS "cash_plan",
       "RSNS" AS "reasons", "REG_DT" AS "created_at"
FROM "TB_INV_STK_DEC";

