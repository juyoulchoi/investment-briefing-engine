ALTER TABLE "TB_BRF_ITEM_DEF"
    DROP CONSTRAINT "uk_brf_item_01";

UPDATE "TB_BRF_ITEM_DEF"
   SET "USE_YN" = 'N',
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "USE_YN" = 'Y';

UPDATE "TB_CD_DTL"
   SET "ACTV_YN" = 'N',
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "CD_GRP" = 'BRIEFING_ITEM'
   AND "ACTV_YN" = 'Y';

INSERT INTO "TB_CD_DTL"
    ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('BRIEFING_ITEM', 'MARKET_RISK', '시장 위험지수', 'DB 확정 시장 위험점수와 위험등급', 1, 'Y'),
    ('BRIEFING_ITEM', 'MARKET_PHASE', '시장 국면', 'DB 확정 시장국면', 2, 'Y'),
    ('BRIEFING_ITEM', 'US_MARKET', '전일 미국시장', '전일 미국 주식 및 주요 시장지표', 3, 'Y'),
    ('BRIEFING_ITEM', 'KR_MARKET_PREVIOUS', '전날 한국 주식시장 상황', '전날 한국 주식시장 확정 데이터', 4, 'Y'),
    ('BRIEFING_ITEM', 'KR_MARKET_OUTLOOK', '한국시장 예상', '확정 데이터에 근거한 한국시장 조건부 예상', 5, 'Y'),
    ('BRIEFING_ITEM', 'MARKET_DIRECTION', '1~4주 시장 방향 예측', 'DB 확정 시장방향지수와 시나리오 확률', 6, 'Y'),
    ('BRIEFING_ITEM', 'SECTOR_SIGNALS', '업종별 신호등', 'DB 확정 업종별 신호', 7, 'Y'),
    ('BRIEFING_ITEM', 'REGULAR_BUY', '정기매수 판단', 'DB 확정 정기매수 판단', 8, 'Y'),
    ('BRIEFING_ITEM', 'ACCOUNT_ACTIONS', '계좌별 행동', '계좌유형별 DB 확정 행동신호', 9, 'Y'),
    ('BRIEFING_ITEM', 'HOLDING_SIGNALS', '보유 종목별 신호등', 'DB 확정 보유종목별 신호', 10, 'Y'),
    ('BRIEFING_ITEM', 'ADDITIONAL_BUYS', '추가매수 후보', 'DB 확정 추가매수 후보', 11, 'Y'),
    ('BRIEFING_ITEM', 'REBUY_SIGNALS', '재매수 신호', 'DB 확정 재매수 신호', 12, 'Y'),
    ('BRIEFING_ITEM', 'ACTION_SIGNAL', '당일 행동신호', 'DB 확정 당일 행동신호', 13, 'Y'),
    ('BRIEFING_ITEM', 'SCHEDULE_AND_RISKS', '주요 일정과 위험요인', '주요 일정과 데이터 기반 위험요인', 14, 'Y'),
    ('BRIEFING_ITEM', 'CONCLUSION', '오늘의 결론', 'DB 확정값을 종합한 설명', 15, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE
SET "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;

INSERT INTO "TB_BRF_ITEM_DEF"
    ("ITEM_CD", "SORT_NO", "ITEM_TP", "USE_YN", "ITEM_DESC", "ITEM_GRP")
SELECT "CD_KEY", "DSP_ORD",
       CASE WHEN "CD_KEY" = 'CONCLUSION' THEN 'FINAL' ELSE 'NORMAL' END,
       'Y', "DESC", "CD_GRP"
  FROM "TB_CD_DTL"
 WHERE "CD_GRP" = 'BRIEFING_ITEM'
   AND "ACTV_YN" = 'Y'
ON CONFLICT ("ITEM_CD") DO UPDATE
SET "SORT_NO" = EXCLUDED."SORT_NO",
    "ITEM_TP" = EXCLUDED."ITEM_TP",
    "USE_YN" = EXCLUDED."USE_YN",
    "ITEM_DESC" = EXCLUDED."ITEM_DESC",
    "MOD_DT" = CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX "UK_TB_BRF_ITEM_DEF_ACTIVE_SORT"
    ON "TB_BRF_ITEM_DEF" ("SORT_NO")
    WHERE "USE_YN" = 'Y';

DO $$
DECLARE
    V_ACTIVE_CODE_COUNT INTEGER;
    V_ACTIVE_ITEM_COUNT INTEGER;
BEGIN
    SELECT COUNT(*) INTO V_ACTIVE_CODE_COUNT
      FROM "TB_CD_DTL"
     WHERE "CD_GRP" = 'BRIEFING_ITEM'
       AND "ACTV_YN" = 'Y';

    SELECT COUNT(*) INTO V_ACTIVE_ITEM_COUNT
      FROM "TB_BRF_ITEM_DEF"
     WHERE "USE_YN" = 'Y';

    IF V_ACTIVE_CODE_COUNT <> 15 OR V_ACTIVE_ITEM_COUNT <> 15 THEN
        RAISE EXCEPTION
            'Standard briefing item catalog mismatch: common codes %, item definitions %',
            V_ACTIVE_CODE_COUNT, V_ACTIVE_ITEM_COUNT;
    END IF;
END $$;
