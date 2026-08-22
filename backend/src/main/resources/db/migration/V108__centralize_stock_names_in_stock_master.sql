-- TB_REG_BUY의 중복 종목명을 TB_STK로 이관한 뒤 제거한다.
-- 해외 종목의 정기매수 한글명을 우선하며, 동일 STK_ID에 복수 이름이 있으면 이관을 중단한다.
DO $$
DECLARE
    V_CONFLICT_COUNT INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO V_CONFLICT_COUNT
      FROM (
          SELECT r."STK_ID"
            FROM "TB_REG_BUY" r
            JOIN "TB_ACCT" a ON a."ACCT_ID" = r."ACCT_ID"
           WHERE a."ACCT_TP" = 'OVERSEAS'
           GROUP BY r."STK_ID"
          HAVING COUNT(DISTINCT r."STK_NM") > 1
      ) conflicts;

    IF V_CONFLICT_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Cannot centralize overseas stock names: conflicting STK_ID count %',
            V_CONFLICT_COUNT;
    END IF;
END $$;

UPDATE "TB_STK" s
   SET "STK_NM" = source."STK_NM",
       "UPD_DTTM" = CURRENT_TIMESTAMP,
       "UPD_USR_ID" = 'SYSTEM'
  FROM (
      SELECT r."STK_ID", MAX(r."STK_NM") AS "STK_NM"
        FROM "TB_REG_BUY" r
        JOIN "TB_ACCT" a ON a."ACCT_ID" = r."ACCT_ID"
       WHERE a."ACCT_TP" = 'OVERSEAS'
       GROUP BY r."STK_ID"
  ) source
 WHERE s."STK_ID" = source."STK_ID"
   AND s."STK_NM" IS DISTINCT FROM source."STK_NM";

DO $$
DECLARE
    V_MISMATCH_COUNT INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO V_MISMATCH_COUNT
      FROM "TB_REG_BUY" r
      JOIN "TB_ACCT" a ON a."ACCT_ID" = r."ACCT_ID"
      JOIN "TB_STK" s ON s."STK_ID" = r."STK_ID"
     WHERE a."ACCT_TP" = 'OVERSEAS'
       AND s."STK_NM" IS DISTINCT FROM r."STK_NM";

    IF V_MISMATCH_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Overseas stock-name migration failed: mismatches %',
            V_MISMATCH_COUNT;
    END IF;
END $$;

CREATE OR REPLACE VIEW tb_reg_buy AS
SELECT r."ACCT_TP" AS account_type,
       r."STK_CD" AS stock_code,
       CAST(s."STK_NM" AS VARCHAR(100)) AS stock_name,
       r."CYCLE_TP" AS cycle_type,
       r."WEEK_DAY" AS day_of_week,
       r."MONTH_DAY" AS day_of_month,
       r."AMT" AS amount,
       r."QTY" AS quantity,
       r."ACTV_YN" AS active_yn,
       r."PAUSE_RSN" AS pause_reason,
       r."START_DT" AS start_date,
       r."END_DT" AS end_date,
       r."REG_DT" AS created_at,
       r."MOD_DT" AS updated_at
  FROM "TB_REG_BUY" r
  JOIN "TB_STK" s ON s."STK_ID" = r."STK_ID";

ALTER TABLE "TB_REG_BUY" DROP COLUMN "STK_NM";

COMMENT ON COLUMN "TB_STK"."STK_NM" IS
    '화면과 업무 API에서 사용하는 종목명 단일 기준';
