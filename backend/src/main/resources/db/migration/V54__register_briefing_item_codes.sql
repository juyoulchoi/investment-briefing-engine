INSERT INTO "TB_COM_CD"
    ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('BRIEFING_ITEM', 'US_STOCK_MKT', '전날 미국 주식시장', '전날 미국 주요 주가지수와 시장 흐름', 1, 'Y'),
    ('BRIEFING_ITEM', 'US_BOND_MKT', '전날 미국 채권시장', '전날 미국 국채금리와 채권시장 흐름', 2, 'Y'),
    ('BRIEFING_ITEM', 'KR_STOCK_MKT', '전날 한국 주식시장', '전날 코스피·코스닥과 투자자별 수급', 3, 'Y'),
    ('BRIEFING_ITEM', 'FX_RATE_CMDTY', '환율·금리·원자재', '환율, 국내외 금리와 주요 원자재 동향', 4, 'Y'),
    ('BRIEFING_ITEM', 'ECON_SCHEDULE', '주요 경제 일정', '당일과 주간 주요 경제지표 및 정책 일정', 5, 'Y'),
    ('BRIEFING_ITEM', 'MKT_RISK', '시장 위험요인', '시장 변동성을 높일 수 있는 핵심 위험요인', 6, 'Y'),
    ('BRIEFING_ITEM', 'MKT_PHASE', '시장국면', '현재 시장국면과 위험수준 판단', 7, 'Y'),
    ('BRIEFING_ITEM', 'REG_BUY_DEC', '정기매수 판단', '정기매수 실행·증액·감액·중단 판단', 8, 'Y'),
    ('BRIEFING_ITEM', 'ADD_BUY_DEC', '추가매수 판단', '확보 현금을 활용한 추가매수 판단', 9, 'Y'),
    ('BRIEFING_ITEM', 'REBUY_SIG', '재매수 신호', '매도 또는 중단 종목의 재매수 가능 신호', 10, 'Y'),
    ('BRIEFING_ITEM', 'ACCT_STRATEGY', '계좌별 전략', '종합·ISA·연금 등 계좌별 운용전략', 11, 'Y'),
    ('BRIEFING_ITEM', 'HOLDING_SIGNAL', '보유종목별 신호등', '보유종목별 위험 및 행동 신호등', 12, 'Y'),
    ('BRIEFING_ITEM', 'TODAY_ACTION', '오늘의 행동신호', '오늘 실행해야 할 투자 행동 요약', 13, 'Y'),
    ('BRIEFING_ITEM', 'CAUTION', '주의사항', '투자 실행 전 확인할 제한사항과 주의점', 14, 'Y'),
    ('BRIEFING_ITEM', 'FINAL_JUDGMENT', '종합판단', '시장·계좌·종목 신호를 결합한 최종 판단', 15, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE
SET "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;

ALTER TABLE "TB_BRF_ITEM"
    ADD COLUMN "ITEM_GRP" VARCHAR(50) NOT NULL DEFAULT 'BRIEFING_ITEM';

ALTER TABLE "TB_BRF_ITEM"
    ADD CONSTRAINT "CK_BRF_ITEM_05" CHECK ("ITEM_GRP" = 'BRIEFING_ITEM'),
    ADD CONSTRAINT "FK_BRF_ITEM_02"
        FOREIGN KEY ("ITEM_GRP", "ITEM_CD")
        REFERENCES "TB_COM_CD" ("CD_GRP", "CD_KEY");

INSERT INTO "TB_BRF_ITEM"
    ("ITEM_CD", "ITEM_NM", "SORT_NO", "ITEM_TP", "USE_YN", "ITEM_DESC")
SELECT
    "CD_KEY",
    "CD_NM",
    "DSP_ORD",
    CASE WHEN "CD_KEY" = 'FINAL_JUDGMENT' THEN 'FINAL' ELSE 'NORMAL' END,
    "ACTV_YN",
    "DESC"
FROM "TB_COM_CD"
WHERE "CD_GRP" = 'BRIEFING_ITEM'
ON CONFLICT ("ITEM_CD") DO UPDATE
SET "ITEM_NM" = EXCLUDED."ITEM_NM",
    "SORT_NO" = EXCLUDED."SORT_NO",
    "ITEM_TP" = EXCLUDED."ITEM_TP",
    "USE_YN" = EXCLUDED."USE_YN",
    "ITEM_DESC" = EXCLUDED."ITEM_DESC",
    "MOD_DT" = CURRENT_TIMESTAMP;

COMMENT ON COLUMN "TB_BRF_ITEM"."ITEM_GRP"
    IS '공통코드 그룹. BRIEFING_ITEM 고정';
