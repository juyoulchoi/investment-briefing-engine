INSERT INTO "TB_CD_DTL"
    ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('DASHBOARD_LABEL', 'KEEP_MINIMUM', '최소 정기매수 유지', '브리핑 종목별 행동신호', 26, 'Y'),
    ('DASHBOARD_LABEL', 'REBUY', '재매수', '브리핑 재매수 신호', 27, 'Y'),
    ('DASHBOARD_LABEL', 'WAIT', '관망', '브리핑 재매수 신호', 28, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE
SET "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;
