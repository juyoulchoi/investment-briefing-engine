CREATE TEMP TABLE TMP_INVESTMENT_GRADE_SCORE (
    "STK_CD" VARCHAR(20) PRIMARY KEY,
    "INV_GRD" VARCHAR(50) NOT NULL,
    "WGT_SCR" INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO TMP_INVESTMENT_GRADE_SCORE ("STK_CD", "INV_GRD", "WGT_SCR")
VALUES
    ('007340', 'S', 8),
    ('105560', 'S', 8),
    ('010120', 'SS', 9),
    ('001440', 'B', 4),
    ('034020', 'AA', 7),
    ('058470', 'SS', 9),
    ('083650', 'S', 8),
    ('028050', 'AA', 7),
    ('005930', 'SSS', 10),
    ('032820', 'B', 4),
    ('000100', 'S', 8),
    ('014680', 'S', 8),
    ('000720', 'AA', 7),
    ('298040', 'SS', 9),
    ('000660', 'SSS', 10),
    ('BOTZ', 'AA', 7),
    ('HYDR', 'B', 4),
    ('QQQ', 'SSS', 10),
    ('SCHD', 'SS', 9),
    ('SMH', 'SS', 9),
    ('SPY', 'SSS', 10),
    ('VIG', 'SS', 9),
    ('XLF', 'S', 8),
    ('XLI', 'S', 8),
    ('XLV', 'S', 8),
    ('GEV', 'S', 8),
    ('MSFT', 'SSS', 10),
    ('BAC', 'A', 6),
    ('BRK.B', 'S', 8),
    ('VRT', 'S', 8),
    ('AVGO', 'SS', 9),
    ('VST', 'S', 8),
    ('V', 'S', 8),
    ('SPCX', 'B', 4),
    ('ANET', 'S', 8),
    ('AMZN', 'SS', 9),
    ('IONQ', 'B', 4),
    ('GOOGL', 'SS', 9),
    ('ABBV', 'AA', 7),
    ('AAPL', 'SS', 9),
    ('NVDA', 'SSS', 10),
    ('WMT', 'SS', 9),
    ('INTC', 'A', 6),
    ('LLY', 'AA', 7),
    ('JPM', 'S', 8),
    ('JNJ', 'S', 8),
    ('CAT', 'AA', 7),
    ('COST', 'S', 8),
    ('CEG', 'S', 8),
    ('PLTR', 'AA', 7),
    ('PLUG', 'B', 4),
    ('411060', 'SSS', 10),
    ('069500', 'SSS', 10),
    ('305720', 'B', 4),
    ('471990', 'SSS', 10),
    ('117700', 'AA', 7),
    ('487230', 'SS', 9),
    ('379800', 'SSS', 10),
    ('144600', 'S', 8),
    ('266420', 'S', 8),
    ('161510', 'SSS', 10),
    ('0023A0', 'CC', 3),
    ('139270', 'S', 8),
    ('227550', 'S', 8),
    ('160580', 'S', 8),
    ('464310', 'S', 8),
    ('329200', 'AA', 7),
    ('458730', 'SSS', 10),
    ('0183J0', 'AA', 7),
    ('305080', 'SSS', 10),
    ('0046A0', 'SSS', 10),
    ('381180', 'SS', 9),
    ('466940', 'SSS', 10),
    ('0089D0', 'SSS', 10),
    ('360750', 'SSS', 10),
    ('455890', 'SS', 9),
    ('0051G0', 'SS', 9),
    ('463250', 'AA', 7),
    ('241180', 'AA', 7),
    ('494670', 'S', 8),
    ('302190', 'SSS', 10);

DO $$
DECLARE
    V_INVALID_CODE_COUNT INTEGER;
    V_UNMAPPED_REG_BUY_COUNT INTEGER;
    V_UNMAPPED_SETTING_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_INVALID_CODE_COUNT
      FROM TMP_INVESTMENT_GRADE_SCORE M
      LEFT JOIN "TB_CD_DTL" C
        ON C."CD_GRP" = 'INVESTMENT_GRADE'
       AND C."CD_NM" = M."INV_GRD"
       AND C."CD_KEY" = M."WGT_SCR"::VARCHAR
       AND C."ACTV_YN" = 'Y'
     WHERE C."CD_KEY" IS NULL;

    SELECT COUNT(*) INTO V_UNMAPPED_REG_BUY_COUNT
      FROM "TB_REG_BUY" R
      LEFT JOIN TMP_INVESTMENT_GRADE_SCORE M ON M."STK_CD" = R."STK_CD"
     WHERE R."DEL_YN" = 'N'
       AND M."STK_CD" IS NULL;

    SELECT COUNT(*) INTO V_UNMAPPED_SETTING_COUNT
      FROM "TB_STK_SET" S
      LEFT JOIN TMP_INVESTMENT_GRADE_SCORE M ON M."STK_CD" = S."STK_CD"
     WHERE M."STK_CD" IS NULL;

    IF V_INVALID_CODE_COUNT <> 0
       OR V_UNMAPPED_REG_BUY_COUNT <> 0
       OR V_UNMAPPED_SETTING_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Investment grade assignment failed: invalid codes %, unmapped regular buys %, unmapped settings %',
            V_INVALID_CODE_COUNT, V_UNMAPPED_REG_BUY_COUNT, V_UNMAPPED_SETTING_COUNT;
    END IF;
END $$;

UPDATE "TB_REG_BUY" R
   SET "INV_GRD" = M."INV_GRD",
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_INVESTMENT_GRADE_SCORE M
 WHERE R."STK_CD" = M."STK_CD"
   AND R."DEL_YN" = 'N';

UPDATE "TB_STK_SET" S
   SET "WGT_SCR" = M."WGT_SCR",
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_INVESTMENT_GRADE_SCORE M
 WHERE S."STK_CD" = M."STK_CD";

CREATE TEMP TABLE TMP_TARGET_WEIGHT ON COMMIT DROP AS
SELECT H."ACCT_TP",
       H."MKT_CD",
       H."STK_CD",
       CASE
           WHEN H."USE_YN" <> 'Y' THEN 0::NUMERIC
           ELSE COALESCE(
               S."WGT_SCR"::NUMERIC
               / NULLIF(
                   SUM(CASE WHEN H."USE_YN" = 'Y' THEN S."WGT_SCR" ELSE 0 END)
                       OVER (PARTITION BY H."ACCT_TP"),
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
   SET "TGT_WGT" = ROUND(T."TGT_WGT", 6),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_TARGET_WEIGHT T
 WHERE S."ACCT_TP" = T."ACCT_TP"
   AND S."MKT_CD" = T."MKT_CD"
   AND S."STK_CD" = T."STK_CD";

UPDATE "TB_HOLD" H
   SET "TGT_WGT" = ROUND(T."TGT_WGT" * 100, 4),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_TARGET_WEIGHT T
 WHERE H."ACCT_TP" = T."ACCT_TP"
   AND H."MKT_CD" = T."MKT_CD"
   AND H."STK_CD" = T."STK_CD"
   AND H."DEL_YN" = 'N';

DO $$
DECLARE
    V_REG_BUY_COUNT INTEGER;
    V_GRADED_REG_BUY_COUNT INTEGER;
    V_SCORE_MISMATCH_COUNT INTEGER;
    V_INVALID_TARGET_COUNT INTEGER;
BEGIN
    SELECT COUNT(*), COUNT(R."INV_GRD")
      INTO V_REG_BUY_COUNT, V_GRADED_REG_BUY_COUNT
      FROM "TB_REG_BUY" R
     WHERE R."DEL_YN" = 'N';

    SELECT COUNT(*) INTO V_SCORE_MISMATCH_COUNT
      FROM "TB_STK_SET" S
      JOIN TMP_INVESTMENT_GRADE_SCORE M ON M."STK_CD" = S."STK_CD"
     WHERE S."WGT_SCR" <> M."WGT_SCR";

    SELECT COUNT(*) INTO V_INVALID_TARGET_COUNT
      FROM (
          SELECT "ACCT_TP"
            FROM TMP_TARGET_WEIGHT
           GROUP BY "ACCT_TP"
          HAVING SUM("TGT_WGT") <> 0
             AND ABS(SUM("TGT_WGT") - 1) > 0.000000001
      ) INVALID_TARGETS;

    IF V_REG_BUY_COUNT <> V_GRADED_REG_BUY_COUNT
       OR V_SCORE_MISMATCH_COUNT <> 0
       OR V_INVALID_TARGET_COUNT <> 0 THEN
        RAISE EXCEPTION
            'Investment grade verification failed: regular buys %, graded %, score mismatches %, invalid targets %',
            V_REG_BUY_COUNT, V_GRADED_REG_BUY_COUNT,
            V_SCORE_MISMATCH_COUNT, V_INVALID_TARGET_COUNT;
    END IF;
END $$;
