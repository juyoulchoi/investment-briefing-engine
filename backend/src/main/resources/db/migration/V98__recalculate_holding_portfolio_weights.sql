WITH ACCOUNT_TOTAL AS (
    SELECT H."ACCT_ID",
           SUM(COALESCE(H."EVL_AMT", 0)) AS "TOT_AST_AMT"
      FROM "TB_HOLD" H
     WHERE H."DEL_YN" = 'N'
       AND H."USE_YN" = 'Y'
     GROUP BY H."ACCT_ID"
), CALCULATED AS (
    SELECT H."HOLD_ID",
           CASE
               WHEN T."TOT_AST_AMT" > 0
                   THEN ROUND(COALESCE(H."EVL_AMT", 0) * 100 / T."TOT_AST_AMT", 4)
               ELSE 0
           END AS "CUR_WGT"
      FROM "TB_HOLD" H
      JOIN ACCOUNT_TOTAL T
        ON T."ACCT_ID" = H."ACCT_ID"
     WHERE H."DEL_YN" = 'N'
       AND H."USE_YN" = 'Y'
)
UPDATE "TB_HOLD" H
   SET "CUR_WGT" = C."CUR_WGT",
       "WGT_DIFF_RT" = CASE
           WHEN H."TGT_WGT" IS NULL OR H."TGT_WGT" <= 0 THEN NULL
           ELSE C."CUR_WGT" - H."TGT_WGT"
       END,
       "WGT_STS" = CASE
           WHEN H."TGT_WGT" IS NULL OR H."TGT_WGT" <= 0 THEN NULL
           WHEN C."CUR_WGT" < H."TGT_WGT" * 0.8 THEN 'UNDERWEIGHT'
           WHEN C."CUR_WGT" > H."TGT_WGT" * 1.2 THEN 'OVERWEIGHT'
           ELSE 'NORMAL'
       END,
       "CALC_DTTM" = CURRENT_TIMESTAMP
  FROM CALCULATED C
 WHERE C."HOLD_ID" = H."HOLD_ID";

UPDATE "TB_HOLD"
   SET "CUR_WGT" = NULL,
       "WGT_DIFF_RT" = NULL,
       "WGT_STS" = NULL
 WHERE "DEL_YN" = 'N'
   AND "USE_YN" <> 'Y';
