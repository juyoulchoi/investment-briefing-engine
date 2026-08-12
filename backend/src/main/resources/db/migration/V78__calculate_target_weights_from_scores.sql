-- Excel 수식과 동일한 목표비중 계산식:
-- IF(사용여부 <> 'Y', 0,
--    IFERROR(비중점수 / SUMIFS(비중점수, 계좌, 현재계좌, 사용여부, 'Y'), 0))

CREATE TEMP TABLE TMP_CALCULATED_TARGET_WEIGHT ON COMMIT DROP AS
SELECT H."ACCT_TP",
       H."MKT_CD",
       H."STK_CD",
       CASE
           WHEN H."USE_YN" <> 'Y' THEN 0::NUMERIC
           ELSE COALESCE(
               S."WGT_SCR"::NUMERIC
               / NULLIF(
                   SUM(
                       CASE WHEN H."USE_YN" = 'Y' THEN S."WGT_SCR" ELSE 0 END
                   ) OVER (PARTITION BY H."ACCT_TP"),
                   0
               ),
               0::NUMERIC
           )
       END AS "TGT_WGT"
  FROM "TB_HOLD" H
  JOIN "TB_STK_SET" S
    ON S."ACCT_TP" = H."ACCT_TP"
   AND S."MKT_CD" = H."MKT_CD"
   AND S."STK_CD" = H."STK_CD"
 WHERE H."DEL_YN" = 'N';

UPDATE "TB_STK_SET" S
   SET "TGT_WGT" = ROUND(C."TGT_WGT", 6),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_CALCULATED_TARGET_WEIGHT C
 WHERE S."ACCT_TP" = C."ACCT_TP"
   AND S."MKT_CD" = C."MKT_CD"
   AND S."STK_CD" = C."STK_CD";

UPDATE "TB_HOLD" H
   SET "TGT_WGT" = ROUND(C."TGT_WGT" * 100, 6),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_CALCULATED_TARGET_WEIGHT C
 WHERE H."ACCT_TP" = C."ACCT_TP"
   AND H."MKT_CD" = C."MKT_CD"
   AND H."STK_CD" = C."STK_CD"
   AND H."DEL_YN" = 'N';

DO $$
DECLARE
    V_INVALID_ACCOUNT_COUNT INTEGER;
    V_INVALID_UNUSED_COUNT INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO V_INVALID_ACCOUNT_COUNT
      FROM (
          SELECT C."ACCT_TP"
            FROM TMP_CALCULATED_TARGET_WEIGHT C
           GROUP BY C."ACCT_TP"
          HAVING SUM(C."TGT_WGT") <> 0
             AND ABS(SUM(C."TGT_WGT") - 1) > 0.000000001
      ) INVALID_ACCOUNTS;

    IF V_INVALID_ACCOUNT_COUNT <> 0 THEN
        RAISE EXCEPTION 'Calculated target weights do not total 100 percent for % accounts',
            V_INVALID_ACCOUNT_COUNT;
    END IF;

    SELECT COUNT(*)
      INTO V_INVALID_UNUSED_COUNT
      FROM TMP_CALCULATED_TARGET_WEIGHT C
      JOIN "TB_HOLD" H
        ON H."ACCT_TP" = C."ACCT_TP"
       AND H."MKT_CD" = C."MKT_CD"
       AND H."STK_CD" = C."STK_CD"
     WHERE H."USE_YN" <> 'Y'
       AND C."TGT_WGT" <> 0;

    IF V_INVALID_UNUSED_COUNT <> 0 THEN
        RAISE EXCEPTION 'Found % unused holdings with non-zero target weights',
            V_INVALID_UNUSED_COUNT;
    END IF;
END $$;
