INSERT INTO "TB_CD_DTL" (
    "CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN"
)
VALUES
    ('BOND_YIELD_SERIES', 'ALL',    '전체 만기',                 '채권금리 화면 전체 시리즈 조회',       0, 'Y'),
    ('BOND_YIELD_SERIES', 'DGS2',   '미국 국채 2년',            '미국 재무부 2년 만기 명목 국채금리',  1, 'Y'),
    ('BOND_YIELD_SERIES', 'DGS10',  '미국 국채 10년',           '미국 재무부 10년 만기 명목 국채금리', 2, 'Y'),
    ('BOND_YIELD_SERIES', 'DGS30',  '미국 국채 30년',           '미국 재무부 30년 만기 명목 국채금리', 3, 'Y'),
    ('BOND_YIELD_SERIES', 'DFII10', '미국 물가연동국채 10년',   '미국 재무부 10년 만기 실질 국채금리', 4, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE SET
    "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;

COMMENT ON COLUMN "TB_BOND_DAY"."BOND_CD" IS
    '채권금리 시리즈 공통코드(BOND_YIELD_SERIES, ALL 제외)';
