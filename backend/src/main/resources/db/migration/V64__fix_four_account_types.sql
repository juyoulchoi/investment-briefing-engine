-- 계좌는 국내, 해외, ISA, 연금의 네 유형만 한 건씩 관리한다.
DO $$
DECLARE
    v_domestic_id BIGINT;
    v_duplicate_id BIGINT;
BEGIN
    SELECT "ACCT_ID" INTO v_domestic_id FROM "TB_ACCT"
     WHERE "ACCT_TP"='DOMESTIC'
     ORDER BY CASE WHEN "ACCT_CD"='DOMESTIC_MAIN' THEN 0 ELSE 1 END, "ACCT_ID" LIMIT 1;

    FOR v_duplicate_id IN
        SELECT "ACCT_ID" FROM "TB_ACCT" WHERE "ACCT_TP"='DOMESTIC' AND "ACCT_ID"<>v_domestic_id
    LOOP
        UPDATE "TB_HOLD" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_REG_BUY" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_CASH_HIS" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_INV_DEC" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_STK_DEC" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_ADD_BUY" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_REBUY" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_REBAL" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_REBAL_ITEM" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;
        UPDATE "TB_BRF" SET "ACCT_ID"=v_domestic_id WHERE "ACCT_ID"=v_duplicate_id;

        UPDATE "TB_CASH_RSV" target SET
            "RSV_AMT"=target."RSV_AMT"+source."RSV_AMT",
            "ACCUM_AMT"=target."ACCUM_AMT"+source."ACCUM_AMT",
            "USED_AMT"=target."USED_AMT"+source."USED_AMT",
            "UPD_DTTM"=CURRENT_TIMESTAMP,"UPD_USR_ID"='SYSTEM'
        FROM "TB_CASH_RSV" source
        WHERE target."ACCT_ID"=v_domestic_id AND source."ACCT_ID"=v_duplicate_id;
        DELETE FROM "TB_CASH_RSV" WHERE "ACCT_ID"=v_duplicate_id;

        UPDATE "TB_ACCT" target SET
            "TOT_AMT"=target."TOT_AMT"+source."TOT_AMT",
            "AVAIL_CASH"=target."AVAIL_CASH"+source."AVAIL_CASH",
            "ADD_INV_CASH"=target."ADD_INV_CASH"+source."ADD_INV_CASH",
            "MAX_INV_AMT"=GREATEST(target."MAX_INV_AMT",source."MAX_INV_AMT"),
            "CASH_AMT"=target."CASH_AMT"+source."CASH_AMT",
            "RSV_CASH_AMT"=target."RSV_CASH_AMT"+source."RSV_CASH_AMT",
            "UPD_DTTM"=CURRENT_TIMESTAMP,"UPD_USR_ID"='SYSTEM'
        FROM "TB_ACCT" source
        WHERE target."ACCT_ID"=v_domestic_id AND source."ACCT_ID"=v_duplicate_id;
        DELETE FROM "TB_ACCT" WHERE "ACCT_ID"=v_duplicate_id;
    END LOOP;
END $$;

DROP VIEW IF EXISTS tb_acct;
ALTER TABLE "TB_ACCT" DROP CONSTRAINT "FK_ACCT_01";
ALTER TABLE "TB_ACCT" DROP CONSTRAINT "CK_ACCT_02";
ALTER TABLE "TB_ACCT" DROP CONSTRAINT "UK_TB_ACCT_01";
ALTER TABLE "TB_ACCT" DROP COLUMN "ACCT_CD", DROP COLUMN "ACCT_NM", DROP COLUMN "ACCT_TP_GRP";
ALTER TABLE "TB_ACCT" ADD CONSTRAINT "UK_TB_ACCT_01" UNIQUE ("ACCT_TP");
ALTER TABLE "TB_ACCT" DROP CONSTRAINT "CK_ACCT_01";
ALTER TABLE "TB_ACCT" ADD CONSTRAINT "CK_ACCT_01" CHECK ("ACCT_TP" IN ('DOMESTIC','OVERSEAS','ISA','PENSION'));
DELETE FROM "TB_CD_DTL" WHERE "CD_GRP"='ACCOUNT_TYPE' AND "CD_KEY"='MIRAE';

CREATE VIEW tb_acct AS
SELECT "ACCT_TP" AS acct_tp,"TOT_AMT" AS total_amt,"AVAIL_CASH" AS avail_cash,
       "CURR" AS currency,"USE_YN" AS use_yn,"REG_DT" AS reg_dt,"MOD_DT" AS mod_dt,
       "ADD_INV_CASH" AS additional_investment_cash,"MAX_INV_AMT" AS max_investment_amount,
       "ACCT_DESC" AS account_description
FROM "TB_ACCT";
COMMENT ON COLUMN "TB_ACCT"."ACCT_TP" IS '계좌 고유 유형. DOMESTIC, OVERSEAS, ISA, PENSION 각 한 건만 허용';
