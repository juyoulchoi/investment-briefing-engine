ALTER TABLE "TB_CD_DTL"
    DROP CONSTRAINT "CK_CD_DTL_INV_GRD_01";

DELETE FROM "TB_CD_DTL"
WHERE "CD_GRP" = 'INVESTMENT_GRADE';

ALTER TABLE "TB_CD_DTL"
    DROP COLUMN "NUM_VAL";

INSERT INTO "TB_CD_DTL" ("CD_GRP", "CD_KEY", "CD_NM", "DESC", "DSP_ORD", "ACTV_YN")
VALUES
    ('INVESTMENT_GRADE', '10', 'SSS', '절대 핵심',       1, 'Y'),
    ('INVESTMENT_GRADE', '9',  'SS',  '최상위 핵심',     2, 'Y'),
    ('INVESTMENT_GRADE', '8',  'S',   '최우선 핵심',     3, 'Y'),
    ('INVESTMENT_GRADE', '7',  'AA',  '상위 핵심',       4, 'Y'),
    ('INVESTMENT_GRADE', '6',  'A',   '핵심',            5, 'Y'),
    ('INVESTMENT_GRADE', '5',  'BB',  '준핵심',          6, 'Y'),
    ('INVESTMENT_GRADE', '4',  'B',   '일반 핵심',       7, 'Y'),
    ('INVESTMENT_GRADE', '3',  'CC',  '핵심·위성 경계',  8, 'Y'),
    ('INVESTMENT_GRADE', '2',  'C',   '상위 위성',       9, 'Y'),
    ('INVESTMENT_GRADE', '0',  'X',   '목표비중 없음',  10, 'Y');

ALTER TABLE "TB_CD_DTL"
    ADD CONSTRAINT "CK_CD_DTL_INV_GRD_01" CHECK (
        "CD_GRP" <> 'INVESTMENT_GRADE' OR "CD_KEY" ~ '^(10|[0-9])$'
    );

CREATE UNIQUE INDEX "UK_CD_DTL_INV_GRD_01"
    ON "TB_CD_DTL" ("CD_NM")
    WHERE "CD_GRP" = 'INVESTMENT_GRADE';
