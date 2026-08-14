ALTER TABLE "TB_CD_DTL"
    ADD COLUMN "NUM_VAL" NUMERIC(20, 4);

COMMENT ON COLUMN "TB_CD_DTL"."NUM_VAL" IS '공통코드 숫자 속성값';

ALTER TABLE "TB_CD_DTL"
    ADD CONSTRAINT "CK_CD_DTL_INV_GRD_01" CHECK (
        "CD_GRP" <> 'INVESTMENT_GRADE'
        OR (
            "NUM_VAL" BETWEEN 0 AND 10
            AND "NUM_VAL" = TRUNC("NUM_VAL")
        )
    );

INSERT INTO "TB_CD_DTL" (
    "CD_GRP", "CD_KEY", "CD_NM", "DESC", "NUM_VAL", "DSP_ORD", "ACTV_YN"
)
VALUES
    ('INVESTMENT_GRADE', 'SSS', 'SSS', '절대 핵심',       10,  1, 'Y'),
    ('INVESTMENT_GRADE', 'SS',  'SS',  '최상위 핵심',      9,  2, 'Y'),
    ('INVESTMENT_GRADE', 'S',   'S',   '최우선 핵심',      8,  3, 'Y'),
    ('INVESTMENT_GRADE', 'AA',  'AA',  '상위 핵심',        7,  4, 'Y'),
    ('INVESTMENT_GRADE', 'A',   'A',   '핵심',             6,  5, 'Y'),
    ('INVESTMENT_GRADE', 'BB',  'BB',  '준핵심',           5,  6, 'Y'),
    ('INVESTMENT_GRADE', 'B',   'B',   '일반 핵심',        4,  7, 'Y'),
    ('INVESTMENT_GRADE', 'CC',  'CC',  '핵심·위성 경계',   3,  8, 'Y'),
    ('INVESTMENT_GRADE', 'C',   'C',   '상위 위성',        2,  9, 'Y'),
    ('INVESTMENT_GRADE', 'X',   'X',   '목표비중 없음',    0, 10, 'Y')
ON CONFLICT ("CD_GRP", "CD_KEY") DO UPDATE SET
    "CD_NM" = EXCLUDED."CD_NM",
    "DESC" = EXCLUDED."DESC",
    "NUM_VAL" = EXCLUDED."NUM_VAL",
    "DSP_ORD" = EXCLUDED."DSP_ORD",
    "ACTV_YN" = EXCLUDED."ACTV_YN",
    "MOD_DT" = CURRENT_TIMESTAMP;
