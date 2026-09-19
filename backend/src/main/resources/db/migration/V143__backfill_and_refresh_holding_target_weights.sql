-- 계좌별 투자등급 점수를 목표비중의 단일 산정 기준으로 사용한다.
-- 신규 보유종목도 TB_STK_SET에 등록하고 계좌 전체를 다시 정규화한다.
INSERT INTO "TB_STK_SET"(
    "ACCT_TP", "MKT_CD", "STK_CD", "WGT_SCR", "TGT_WGT", "REG_DT", "MOD_DT")
SELECT a."ACCT_TP",
       s."MKT_CD",
       s."STK_CD",
       CAST(c."CD_KEY" AS INTEGER),
       NULL,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
  FROM "TB_HOLD" h
  JOIN "TB_ACCT" a ON a."ACCT_ID" = h."ACCT_ID"
  JOIN "TB_STK" s ON s."STK_ID" = h."STK_ID"
  LEFT JOIN "TB_REG_BUY" r
    ON r."ACCT_ID" = h."ACCT_ID"
   AND r."STK_ID" = h."STK_ID"
   AND r."DEL_YN" = 'N'
  LEFT JOIN "TB_CD_DTL" c
    ON c."CD_GRP" = 'INVESTMENT_GRADE'
   AND c."CD_NM" = r."INV_GRD"
   AND c."ACTV_YN" = 'Y'
 WHERE h."DEL_YN" = 'N'
ON CONFLICT ("ACCT_TP", "STK_CD") DO UPDATE
   SET "MKT_CD" = EXCLUDED."MKT_CD",
       "WGT_SCR" = EXCLUDED."WGT_SCR",
       "MOD_DT" = CURRENT_TIMESTAMP;

WITH scored AS (
    SELECT h."HOLD_ID",
           a."ACCT_ID",
           a."ACCT_TP",
           s."STK_CD",
           h."USE_YN",
           ss."WGT_SCR",
           SUM(CASE WHEN h."USE_YN" = 'Y' THEN COALESCE(ss."WGT_SCR", 0) ELSE 0 END)
               OVER (PARTITION BY a."ACCT_ID") AS total_score
      FROM "TB_HOLD" h
      JOIN "TB_ACCT" a ON a."ACCT_ID" = h."ACCT_ID"
      JOIN "TB_STK" s ON s."STK_ID" = h."STK_ID"
      JOIN "TB_STK_SET" ss
        ON ss."ACCT_TP" = a."ACCT_TP"
       AND ss."STK_CD" = s."STK_CD"
     WHERE h."DEL_YN" = 'N'
), calculated AS (
    SELECT "HOLD_ID",
           "ACCT_TP",
           "STK_CD",
           CASE
               WHEN "USE_YN" <> 'Y' THEN 0::NUMERIC
               WHEN "WGT_SCR" IS NULL OR total_score = 0 THEN NULL
               ELSE "WGT_SCR"::NUMERIC / total_score
           END AS target_weight
      FROM scored
), updated_settings AS (
    UPDATE "TB_STK_SET" ss
       SET "TGT_WGT" = ROUND(c.target_weight, 6),
           "MOD_DT" = CURRENT_TIMESTAMP
      FROM calculated c
     WHERE ss."ACCT_TP" = c."ACCT_TP"
       AND ss."STK_CD" = c."STK_CD"
    RETURNING ss."ACCT_TP", ss."STK_CD"
)
UPDATE "TB_HOLD" h
   SET "TGT_WGT" = ROUND(c.target_weight * 100, 4),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM calculated c
 WHERE h."HOLD_ID" = c."HOLD_ID";

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
       "WGT_DIFF_RT" = CASE
           WHEN c."TGT_WGT" IS NULL OR c."TGT_WGT" = 0 THEN NULL
           ELSE c.current_weight - c."TGT_WGT"
       END,
       "WGT_STS" = CASE
           WHEN c."TGT_WGT" IS NULL OR c."TGT_WGT" = 0 THEN NULL
           WHEN c.current_weight < c."TGT_WGT" * 0.8 THEN 'UNDERWEIGHT'
           WHEN c.current_weight > c."TGT_WGT" * 1.2 THEN 'OVERWEIGHT'
           ELSE 'NORMAL'
       END,
       "CALC_DTTM" = CURRENT_TIMESTAMP,
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM calculated c
 WHERE c."HOLD_ID" = h."HOLD_ID";

UPDATE "TB_HOLD"
   SET "CUR_WGT" = NULL,
       "WGT_DIFF_RT" = NULL,
       "WGT_STS" = NULL,
       "CALC_DTTM" = CURRENT_TIMESTAMP,
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "DEL_YN" = 'N'
   AND "USE_YN" <> 'Y';

DO $$
DECLARE
    v_missing_setting_count INTEGER;
    v_invalid_target_sum_count INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_missing_setting_count
      FROM "TB_HOLD" h
      JOIN "TB_ACCT" a ON a."ACCT_ID" = h."ACCT_ID"
      JOIN "TB_STK" s ON s."STK_ID" = h."STK_ID"
      LEFT JOIN "TB_STK_SET" ss
        ON ss."ACCT_TP" = a."ACCT_TP"
       AND ss."STK_CD" = s."STK_CD"
     WHERE h."DEL_YN" = 'N'
       AND ss."STK_CD" IS NULL;

    SELECT COUNT(*)
      INTO v_invalid_target_sum_count
      FROM (
          SELECT h."ACCT_ID"
            FROM "TB_HOLD" h
           WHERE h."DEL_YN" = 'N'
             AND h."USE_YN" = 'Y'
             AND h."TGT_WGT" IS NOT NULL
           GROUP BY h."ACCT_ID"
          HAVING ABS(SUM(h."TGT_WGT") - 100) > 0.01
      ) invalid_accounts;

    IF v_missing_setting_count <> 0 OR v_invalid_target_sum_count <> 0 THEN
        RAISE EXCEPTION
            'Target-weight refresh failed: missing settings %, invalid account sums %',
            v_missing_setting_count,
            v_invalid_target_sum_count;
    END IF;
END $$;
