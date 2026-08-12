-- 정기매수 설정은 보유종목의 하위 설정으로 관리한다.
-- 행 존재 여부가 설정 존재 여부이며, 실행 여부는 BUY_STS로만 판단한다.

-- 기존 보유종목 중 정기매수 설정이 없는 종목도 기본 일시정지 설정으로 등록한다.
INSERT INTO "TB_REG_BUY" (
    "ACCT_TP", "STK_CD", "STK_NM", "CYCLE_TP", "MONTH_DAY", "ACTV_YN",
    "ACCT_ID", "STK_ID", "BUY_CYCLE", "BUY_DAY_NOS", "BUY_BASIS",
    "MIN_BUY_AMT", "MAX_MULT", "BUY_STS", "PAUSE_RSN", "USER_PAUSE_YN",
    "AUTO_CALC_YN", "RULE_VER_NO", "USE_YN", "DEL_YN"
)
SELECT A."ACCT_TP", S."STK_CD", S."STK_NM", 'MONTHLY', 15, 'N',
       H."ACCT_ID", H."STK_ID", 'MONTHLY', '15', 'AMOUNT',
       0, 3, 'PAUSED', '기본 설정', 'N', 'Y', 1, 'Y', 'N'
  FROM "TB_HOLD" H
  JOIN "TB_ACCT" A ON A."ACCT_ID" = H."ACCT_ID"
  JOIN "TB_STK" S ON S."STK_ID" = H."STK_ID"
  LEFT JOIN "TB_REG_BUY" R
    ON R."ACCT_ID" = H."ACCT_ID"
   AND R."STK_ID" = H."STK_ID"
 WHERE H."DEL_YN" = 'N'
   AND R."ACCT_ID" IS NULL;

-- 제거 전 기존 94건을 포함한 모든 행을 Y로 정규화한다.
UPDATE "TB_REG_BUY" SET "USE_YN" = 'Y';

DROP INDEX IF EXISTS "IDX_TB_REG_BUY_03";
ALTER TABLE "TB_REG_BUY" DROP CONSTRAINT IF EXISTS "CK_TB_REG_BUY_14";
ALTER TABLE "TB_REG_BUY" DROP COLUMN "USE_YN";
CREATE INDEX "IDX_TB_REG_BUY_03"
    ON "TB_REG_BUY" ("BUY_STS", "DEL_YN");

-- 정기매수 설정은 반드시 같은 계좌의 보유종목을 참조해야 한다.
ALTER TABLE "TB_REG_BUY"
    ADD CONSTRAINT "FK_TB_REG_BUY_03"
        FOREIGN KEY ("ACCT_ID", "STK_ID")
        REFERENCES "TB_HOLD" ("ACCT_ID", "STK_ID")
        ON UPDATE CASCADE
        ON DELETE CASCADE;

-- 목표비중은 정기매수 상태와 무관하게 보유종목 사용여부 및 비중점수로 계산한다.
CREATE TEMP TABLE TMP_HOLDING_TARGET_WEIGHT ON COMMIT DROP AS
SELECT H."ACCT_TP",
       H."MKT_CD",
       H."STK_CD",
       CASE
           WHEN H."USE_YN" = 'Y' THEN COALESCE(
               SS."WGT_SCR"::NUMERIC
               / NULLIF(
                   SUM(CASE WHEN H."USE_YN" = 'Y' THEN SS."WGT_SCR" ELSE 0 END)
                       OVER (PARTITION BY H."ACCT_TP"),
                   0
               ),
               0::NUMERIC
           )
           ELSE 0::NUMERIC
       END AS "TGT_WGT"
  FROM "TB_HOLD" H
  JOIN "TB_STK_SET" SS
    ON SS."ACCT_TP" = H."ACCT_TP"
   AND SS."MKT_CD" = H."MKT_CD"
   AND SS."STK_CD" = H."STK_CD"
 WHERE H."DEL_YN" = 'N';

UPDATE "TB_STK_SET" SS
   SET "TGT_WGT" = ROUND(C."TGT_WGT", 6),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_HOLDING_TARGET_WEIGHT C
 WHERE SS."ACCT_TP" = C."ACCT_TP"
   AND SS."MKT_CD" = C."MKT_CD"
   AND SS."STK_CD" = C."STK_CD";

UPDATE "TB_HOLD" H
   SET "TGT_WGT" = ROUND(C."TGT_WGT" * 100, 4),
       "MOD_DT" = CURRENT_TIMESTAMP
  FROM TMP_HOLDING_TARGET_WEIGHT C
 WHERE H."ACCT_TP" = C."ACCT_TP"
   AND H."MKT_CD" = C."MKT_CD"
   AND H."STK_CD" = C."STK_CD"
   AND H."DEL_YN" = 'N';

DO $$
DECLARE
    V_HOLD_COUNT INTEGER;
    V_REG_BUY_COUNT INTEGER;
    V_ORPHAN_COUNT INTEGER;
    V_INVALID_TARGET_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_HOLD_COUNT
      FROM "TB_HOLD" WHERE "DEL_YN" = 'N';
    SELECT COUNT(*) INTO V_REG_BUY_COUNT
      FROM "TB_REG_BUY" WHERE "DEL_YN" = 'N';
    SELECT COUNT(*) INTO V_ORPHAN_COUNT
      FROM "TB_REG_BUY" R
      LEFT JOIN "TB_HOLD" H
        ON H."ACCT_ID" = R."ACCT_ID"
       AND H."STK_ID" = R."STK_ID"
       AND H."DEL_YN" = 'N'
     WHERE R."DEL_YN" = 'N'
       AND H."HOLD_ID" IS NULL;

    IF V_HOLD_COUNT <> V_REG_BUY_COUNT OR V_ORPHAN_COUNT <> 0 THEN
        RAISE EXCEPTION 'Holding/regular-buy alignment failed: holdings %, regular buys %, orphans %',
            V_HOLD_COUNT, V_REG_BUY_COUNT, V_ORPHAN_COUNT;
    END IF;

    SELECT COUNT(*) INTO V_INVALID_TARGET_COUNT
      FROM (
          SELECT "ACCT_TP"
            FROM TMP_HOLDING_TARGET_WEIGHT
           GROUP BY "ACCT_TP"
          HAVING SUM("TGT_WGT") <> 0
             AND ABS(SUM("TGT_WGT") - 1) > 0.000000001
      ) INVALID_TARGETS;

    IF V_INVALID_TARGET_COUNT <> 0 THEN
        RAISE EXCEPTION 'Holding target weights do not total 100 percent for % accounts',
            V_INVALID_TARGET_COUNT;
    END IF;
END $$;
