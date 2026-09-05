INSERT INTO "TB_CD_DTL"
    ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('DASHBOARD_LABEL', 'DAILY', '일일', '브리핑 유형', 41, 'Y'),
    ('DASHBOARD_LABEL', 'WEEKLY', '주간', '브리핑 유형', 42, 'Y'),
    ('DASHBOARD_LABEL', 'MONTHLY', '월간', '브리핑 유형', 43, 'Y'),
    ('DASHBOARD_LABEL', 'READY', '준비', '브리핑 상태', 51, 'Y'),
    ('DASHBOARD_LABEL', 'GENERATING', '생성 중', '브리핑 상태', 52, 'Y'),
    ('DASHBOARD_LABEL', 'GENERATED', '생성', '브리핑 상태', 53, 'Y'),
    ('DASHBOARD_LABEL', 'REVIEWED', '검토', '브리핑 상태', 54, 'Y'),
    ('DASHBOARD_LABEL', 'PUBLISHED', '발행', '브리핑 상태', 55, 'Y'),
    ('DASHBOARD_LABEL', 'FAILED', '실패', '브리핑 상태', 56, 'Y'),
    ('DASHBOARD_LABEL', 'CANCELLED', '취소', '브리핑 상태', 57, 'Y'),
    ('DASHBOARD_LABEL', 'WATCH', '관찰', '브리핑 신호', 61, 'Y'),
    ('DASHBOARD_LABEL', 'CAUTION', '주의', '브리핑 신호', 62, 'Y'),
    ('DASHBOARD_LABEL', 'RISK', '위험', '브리핑 신호', 63, 'Y'),
    ('DASHBOARD_LABEL', 'CRITICAL', '매우 높음', '위험등급', 64, 'Y'),
    ('DASHBOARD_LABEL', 'OVERHEATED', '과열', '시장국면', 71, 'Y'),
    ('DASHBOARD_LABEL', 'STRONG_CORRECTION', '강한 조정', '시장국면', 72, 'Y'),
    ('DASHBOARD_LABEL', 'CRASH_RISK', '급락 위험', '시장국면', 73, 'Y'),
    ('DASHBOARD_LABEL', 'HOLD_CASH', '현금 유지', '투자 행동신호', 81, 'Y'),
    ('DASHBOARD_LABEL', 'REDUCE_REGULAR_BUY', '정기매수 축소', '투자 행동신호', 82, 'Y'),
    ('DASHBOARD_LABEL', 'INCREASE_REGULAR_BUY', '정기매수 확대', '투자 행동신호', 83, 'Y'),
    ('DASHBOARD_LABEL', 'SELECTIVE_ADD_BUY', '선별 추가매수', '투자 행동신호', 84, 'Y'),
    ('DASHBOARD_LABEL', 'PAUSE_BUY', '매수 일시중지', '투자 행동신호', 85, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE
SET "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;
