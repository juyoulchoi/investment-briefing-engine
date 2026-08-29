INSERT INTO "TB_CD_DTL"
    ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('REG_BUY_PAUSE_REASON', 'WEIGHT_NORMAL', '비중 적정', '현재 비중이 적정하여 모으기를 중지', 1, 'Y'),
    ('REG_BUY_PAUSE_REASON', 'WEIGHT_EXCEEDED', '비중 초과', '현재 비중이 목표 비중을 초과', 2, 'Y'),
    ('REG_BUY_PAUSE_REASON', 'BUY_CONDITION_NOT_MET', '매수 조건 미충족', '정기매수 조건 미충족', 3, 'Y'),
    ('REG_BUY_PAUSE_REASON', 'STRATEGY_CHANGED', '투자 전략 변경', '투자 전략 변경으로 모으기를 중지', 4, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE
SET "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;

DELETE FROM "TB_CD_DTL"
 WHERE "CD_GRP" = 'REG_BUY_PAUSE_REASON'
   AND ("CD_KEY" = 'USER_PAUSE' OR "CD_NM" = '사용자 일시정지');

UPDATE "TB_REG_BUY"
   SET "PAUSE_RSN" = '비중 적정',
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "PAUSE_RSN" IN ('기본 설정', '기본설정');

UPDATE "TB_REG_BUY"
   SET "PAUSE_RSN" = NULL,
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "PAUSE_RSN" = '사용자 일시정지';

UPDATE "TB_REG_BUY"
   SET "WEEK_DAY" = NULL,
       "MONTH_DAY" = NULL,
       "APPLIED_DAY_NOS" = NULL,
       "MOD_DT" = CURRENT_TIMESTAMP
 WHERE "BUY_STS" = 'STOPPED'
   AND "USER_PAUSE_YN" = 'Y';
