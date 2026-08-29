WITH exchange_rate AS (
    SELECT COALESCE((
        SELECT "EXCH_RT"
          FROM "TB_EXCH_DAY"
         WHERE "BASE_CURR_CD" = 'USD'
           AND "QUOTE_CURR_CD" = 'KRW'
         ORDER BY "BASE_DT" DESC
         LIMIT 1
    ), 1) AS usd_krw
), normalized AS (
    SELECT h."HOLD_ID",
           h."TGT_WGT",
           CASE WHEN a."ACCT_TP" = 'OVERSEAS'
                THEN COALESCE(h."ORG_EVL_AMT", h."EVL_AMT", 0) * x.usd_krw
                ELSE COALESCE(h."EVL_AMT", 0)
           END AS evaluation_amount,
           SUM(CASE WHEN a."ACCT_TP" = 'OVERSEAS'
                    THEN COALESCE(h."ORG_EVL_AMT", h."EVL_AMT", 0) * x.usd_krw
                    ELSE COALESCE(h."EVL_AMT", 0)
               END) OVER (PARTITION BY h."ACCT_ID") + a."CASH_AMT" AS total_asset
      FROM "TB_HOLD" h
      JOIN "TB_ACCT" a ON a."ACCT_ID" = h."ACCT_ID"
      CROSS JOIN exchange_rate x
     WHERE h."DEL_YN" = 'N'
       AND h."USE_YN" = 'Y'
), calculated AS (
    SELECT "HOLD_ID",
           "TGT_WGT",
           CASE WHEN total_asset > 0
                THEN ROUND(evaluation_amount * 100 / total_asset, 4)
           END AS current_weight
      FROM normalized
)
UPDATE "TB_HOLD" h
   SET "CUR_WGT" = c.current_weight,
       "WGT_DIFF_RT" = CASE WHEN c."TGT_WGT" IS NULL OR c."TGT_WGT" = 0 THEN NULL
                             ELSE c.current_weight - c."TGT_WGT" END,
       "WGT_STS" = CASE WHEN c."TGT_WGT" IS NULL OR c."TGT_WGT" = 0 THEN NULL
                         WHEN c.current_weight < c."TGT_WGT" * 0.8 THEN 'UNDERWEIGHT'
                         WHEN c.current_weight > c."TGT_WGT" * 1.2 THEN 'OVERWEIGHT'
                         ELSE 'NORMAL' END,
       "CALC_DTTM" = CURRENT_TIMESTAMP
  FROM calculated c
 WHERE c."HOLD_ID" = h."HOLD_ID";

COMMENT ON COLUMN "TB_HOLD"."CUR_WGT" IS
    '계좌 전체 비중: 종목 평가금액 / (전체 활성 종목 평가금액 + 예수금) * 100';
