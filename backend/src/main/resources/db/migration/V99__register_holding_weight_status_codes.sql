INSERT INTO "TB_CD_DTL" (
    "CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN"
)
VALUES
    ('WGT_STS', 'OVERWEIGHT',  '비중초과', '현재비중이 목표비중의 120%를 초과', 1, 'Y'),
    ('WGT_STS', 'UNDERWEIGHT', '비중부족', '현재비중이 목표비중의 80% 미만',   2, 'Y'),
    ('WGT_STS', 'NORMAL',      '적정',     '현재비중이 목표비중의 적정 범위',  3, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE SET
    "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;

COMMENT ON COLUMN "TB_HOLD"."WGT_STS" IS
    '비중상태 공통코드(WGT_STS): OVERWEIGHT, UNDERWEIGHT, NORMAL';
