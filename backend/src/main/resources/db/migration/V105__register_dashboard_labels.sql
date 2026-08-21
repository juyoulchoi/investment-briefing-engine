INSERT INTO "TB_CD_DTL"
    ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('DASHBOARD_LABEL', 'GREED', '탐욕', '대시보드 시장심리 단계', 1, 'Y'),
    ('DASHBOARD_LABEL', 'OPTIMISM', '낙관', '대시보드 시장심리 단계', 2, 'Y'),
    ('DASHBOARD_LABEL', 'NEUTRAL', '중립', '대시보드 시장심리 단계', 3, 'Y'),
    ('DASHBOARD_LABEL', 'FATIGUE', '피로', '대시보드 시장심리 단계', 4, 'Y'),
    ('DASHBOARD_LABEL', 'FEAR', '공포', '대시보드 시장심리 단계', 5, 'Y'),
    ('DASHBOARD_LABEL', 'PANIC', '패닉', '대시보드 시장심리 단계', 6, 'Y'),
    ('DASHBOARD_LABEL', 'STRONG_BULL', '강한 상승', '대시보드 시장국면', 11, 'Y'),
    ('DASHBOARD_LABEL', 'BULL', '상승', '대시보드 시장국면', 12, 'Y'),
    ('DASHBOARD_LABEL', 'NORMAL', '정상', '대시보드 시장국면', 13, 'Y'),
    ('DASHBOARD_LABEL', 'MILD_CORRECTION', '완만한 조정', '대시보드 시장국면', 14, 'Y'),
    ('DASHBOARD_LABEL', 'CORRECTION', '조정', '대시보드 시장국면', 15, 'Y'),
    ('DASHBOARD_LABEL', 'BEAR', '약세', '대시보드 시장국면', 16, 'Y'),
    ('DASHBOARD_LABEL', 'KEEP_REGULAR_BUY', '정기매수 유지', '대시보드 투자 행동신호', 21, 'Y'),
    ('DASHBOARD_LABEL', 'INCREASE', '매수 확대', '대시보드 투자 행동신호', 22, 'Y'),
    ('DASHBOARD_LABEL', 'REDUCE', '매수 축소', '대시보드 투자 행동신호', 23, 'Y'),
    ('DASHBOARD_LABEL', 'PAUSE', '매수 중지', '대시보드 투자 행동신호', 24, 'Y'),
    ('DASHBOARD_LABEL', 'HOLD', '관망', '대시보드 투자 행동신호', 25, 'Y'),
    ('DASHBOARD_LABEL', 'LOW', '낮음', '대시보드 위험등급', 31, 'Y'),
    ('DASHBOARD_LABEL', 'MEDIUM', '보통', '대시보드 위험등급', 32, 'Y'),
    ('DASHBOARD_LABEL', 'HIGH', '높음', '대시보드 위험등급', 33, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE
SET "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;
