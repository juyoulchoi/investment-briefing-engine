DROP VIEW IF EXISTS tb_acct;

DROP INDEX IF EXISTS "IDX_TB_ACCT_02";
ALTER TABLE "TB_ACCT" DROP CONSTRAINT IF EXISTS "CK_TB_ACCT_06";
ALTER TABLE "TB_ACCT"
    DROP COLUMN "BRKR_CD",
    DROP COLUMN "BRKR_NM",
    DROP COLUMN "ACCT_NO_MASK",
    DROP COLUMN "USE_YN";

CREATE VIEW tb_acct AS
SELECT "ACCT_TP" AS acct_tp,
       "TOT_AMT" AS total_amt,
       "AVAIL_CASH" AS avail_cash,
       "CURR" AS currency,
       "REG_DT" AS reg_dt,
       "MOD_DT" AS mod_dt,
       "ADD_INV_CASH" AS additional_investment_cash,
       "MAX_INV_AMT" AS max_investment_amount,
       "ACCT_DESC" AS account_description
  FROM "TB_ACCT";

COMMENT ON COLUMN "TB_ACCT"."CASH_AMT" IS '예수금';
COMMENT ON COLUMN "TB_ACCT"."RSV_CASH_AMT" IS '예수금에서 추가 매수에 사용할 대기 현금';
COMMENT ON COLUMN "TB_ACCT"."TGT_CASH_WGT" IS '전체 평가 금액에서 현금으로 유지할 목표 비중';
