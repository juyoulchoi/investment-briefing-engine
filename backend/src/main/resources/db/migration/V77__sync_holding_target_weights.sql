-- 투자 설정 화면은 보유종목 목표비중을 우선 표시하므로,
-- V76에서 확정한 100개 종목의 목표비중을 보유종목에도 동일하게 반영한다.
UPDATE "TB_HOLD" H
   SET "TGT_WGT" = S."TGT_WGT" * 100,
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM "TB_STK_SET" S
 WHERE H."ACCT_TP" = S."ACCT_TP"
   AND H."MKT_CD" = S."MKT_CD"
   AND H."STK_CD" = S."STK_CD"
   AND H."DEL_YN" = 'N'
   AND S."WGT_SCR" IS NOT NULL;

DO $$
DECLARE
    V_SYNCED_COUNT INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO V_SYNCED_COUNT
      FROM "TB_HOLD" H
      JOIN "TB_STK_SET" S
        ON S."ACCT_TP" = H."ACCT_TP"
       AND S."MKT_CD" = H."MKT_CD"
       AND S."STK_CD" = H."STK_CD"
     WHERE H."DEL_YN" = 'N'
       AND S."WGT_SCR" IS NOT NULL
       AND H."TGT_WGT" = S."TGT_WGT" * 100;

    IF V_SYNCED_COUNT <> 100 THEN
        RAISE EXCEPTION 'Expected 100 synchronized holding targets, found %',
            V_SYNCED_COUNT;
    END IF;
END $$;
