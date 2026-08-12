-- 투자 설정 관리의 사용여부(TB_REG_BUY.USE_YN)를 기준으로 목표비중을 계산한다.
-- 미사용 또는 정기매수 미등록 종목은 0%, 사용 종목은 계좌별 비중점수 합계로 나눈다.

CREATE TEMP TABLE TMP_REGULAR_BUY_TARGET_WEIGHT ON COMMIT DROP AS
SELECT S."ACCT_TP",
       S."MKT_CD",
       S."STK_CD",
       CASE
           WHEN R."USE_YN" = 'Y' THEN COALESCE(
               S."WGT_SCR"::NUMERIC
               / NULLIF(
                   SUM(
                       CASE WHEN R."USE_YN" = 'Y' THEN S."WGT_SCR" ELSE 0 END
                   ) OVER (PARTITION BY S."ACCT_TP"),
                   0
               ),
               0::NUMERIC
           )
           ELSE 0::NUMERIC
       END AS "TGT_WGT"
  FROM "TB_STK_SET" S
  LEFT JOIN "TB_REG_BUY" R
    ON R."ACCT_TP" = S."ACCT_TP"
   AND R."STK_CD" = S."STK_CD"
   AND R."DEL_YN" = 'N';

UPDATE "TB_STK_SET" S
   SET "TGT_WGT" = ROUND(C."TGT_WGT", 6),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_REGULAR_BUY_TARGET_WEIGHT C
 WHERE S."ACCT_TP" = C."ACCT_TP"
   AND S."MKT_CD" = C."MKT_CD"
   AND S."STK_CD" = C."STK_CD";

UPDATE "TB_HOLD" H
   SET "TGT_WGT" = ROUND(C."TGT_WGT" * 100, 6),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_REGULAR_BUY_TARGET_WEIGHT C
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
            FROM TMP_REGULAR_BUY_TARGET_WEIGHT C
           GROUP BY C."ACCT_TP"
          HAVING SUM(C."TGT_WGT") <> 0
             AND ABS(SUM(C."TGT_WGT") - 1) > 0.000000001
      ) INVALID_ACCOUNTS;

    IF V_INVALID_ACCOUNT_COUNT <> 0 THEN
        RAISE EXCEPTION 'Calculated regular-buy targets do not total 100 percent for % accounts',
            V_INVALID_ACCOUNT_COUNT;
    END IF;

    SELECT COUNT(*)
      INTO V_INVALID_UNUSED_COUNT
      FROM TMP_REGULAR_BUY_TARGET_WEIGHT C
      LEFT JOIN "TB_REG_BUY" R
        ON R."ACCT_TP" = C."ACCT_TP"
       AND R."STK_CD" = C."STK_CD"
       AND R."DEL_YN" = 'N'
     WHERE COALESCE(R."USE_YN", 'N') <> 'Y'
       AND C."TGT_WGT" <> 0;

    IF V_INVALID_UNUSED_COUNT <> 0 THEN
        RAISE EXCEPTION 'Found % unused regular-buy rows with non-zero target weights',
            V_INVALID_UNUSED_COUNT;
    END IF;
END $$;
