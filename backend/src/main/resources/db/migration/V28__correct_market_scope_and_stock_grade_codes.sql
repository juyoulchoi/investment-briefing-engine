DROP VIEW IF EXISTS vw_account_stock;

INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
VALUES
    ('LISTING_SCOPE', 'DOMESTIC', '국내 상장', '국내 거래소 상장 종목', 1, 'Y'),
    ('LISTING_SCOPE', 'OVERSEAS', '해외 상장', '해외 거래소 상장 종목', 2, 'Y'),
    ('MARKET_SCOPE', 'GENERAL', '종합계좌', '해외주식 등을 보유하는 종합계좌', 1, 'Y'),
    ('MARKET_SCOPE', 'DOMESTIC', '국내계좌', '국내 개별주식 계좌', 2, 'Y'),
    ('MARKET_SCOPE', 'ISA', 'ISA계좌', '개인종합자산관리계좌', 3, 'Y'),
    ('MARKET_SCOPE', 'PENSION', '연금계좌', '연금 투자계좌', 4, 'Y'),
    ('STOCK_GRADE', 'CORE', '핵심', '장기 보유와 포트폴리오 중심 종목', 1, 'Y'),
    ('STOCK_GRADE', 'SATELLITE', '위성', '핵심 자산을 보완하는 종목', 2, 'Y'),
    ('STOCK_GRADE', 'THEME', '테마', '특정 산업 또는 테마 집중 종목', 3, 'Y'),
    ('STOCK_GRADE', 'CASH_LIKE', '현금성', '단기 자금 보관과 유동성 확보 자산', 4, 'Y')
ON CONFLICT (code_group, code_key) DO UPDATE
SET code_name = EXCLUDED.code_name,
    description = EXCLUDED.description,
    display_order = EXCLUDED.display_order,
    active_yn = EXCLUDED.active_yn;

ALTER TABLE tb_stock
    DROP CONSTRAINT fk_stock_market_scope_common_code,
    DROP CONSTRAINT ck_stock_market_scope_group,
    DROP CONSTRAINT ck_stock_market_scope;

ALTER TABLE tb_stock
    RENAME COLUMN market_scope TO listing_scope;
ALTER TABLE tb_stock
    RENAME COLUMN market_scope_group TO listing_scope_group;

ALTER TABLE tb_stock
    ALTER COLUMN listing_scope_group SET DEFAULT 'LISTING_SCOPE';
UPDATE tb_stock SET listing_scope_group = 'LISTING_SCOPE';

ALTER TABLE tb_stock
    ADD CONSTRAINT ck_stock_listing_scope_group
        CHECK (listing_scope_group = 'LISTING_SCOPE'),
    ADD CONSTRAINT ck_stock_listing_scope
        CHECK (listing_scope IN ('DOMESTIC', 'OVERSEAS')),
    ADD CONSTRAINT fk_stock_listing_scope_common_code
        FOREIGN KEY (listing_scope_group, listing_scope)
        REFERENCES tb_common_code (code_group, code_key);

ALTER TABLE tb_stock_holding
    DROP CONSTRAINT fk_stock_holding_account_code,
    DROP CONSTRAINT fk_stock_holding_role_code,
    DROP CONSTRAINT ck_stock_holding_account_group,
    DROP CONSTRAINT ck_stock_holding_role_group;

ALTER TABLE tb_stock_holding
    RENAME COLUMN account_type_group TO market_scope_group;
ALTER TABLE tb_stock_holding
    RENAME COLUMN account_type TO market_scope;

ALTER TABLE tb_stock_holding
    ALTER COLUMN market_scope_group SET DEFAULT 'MARKET_SCOPE',
    ALTER COLUMN stock_grade_group SET DEFAULT 'STOCK_GRADE';
UPDATE tb_stock_holding
SET market_scope_group = 'MARKET_SCOPE',
    stock_grade_group = 'STOCK_GRADE';

ALTER TABLE tb_stock_holding
    ADD CONSTRAINT ck_stock_holding_market_scope_group
        CHECK (market_scope_group = 'MARKET_SCOPE'),
    ADD CONSTRAINT ck_stock_holding_stock_grade_group
        CHECK (stock_grade_group = 'STOCK_GRADE'),
    ADD CONSTRAINT fk_stock_holding_market_scope_code
        FOREIGN KEY (market_scope_group, market_scope)
        REFERENCES tb_common_code (code_group, code_key),
    ADD CONSTRAINT fk_stock_holding_stock_grade_code
        FOREIGN KEY (stock_grade_group, stock_grade)
        REFERENCES tb_common_code (code_group, code_key);

DELETE FROM tb_common_code
WHERE code_group IN ('STOCK_MARKET_SCOPE', 'PORTFOLIO_ROLE');

COMMENT ON COLUMN tb_stock.listing_scope_group IS '상장시장 공통 코드 그룹키: LISTING_SCOPE';
COMMENT ON COLUMN tb_stock.listing_scope IS '상장시장 코드: DOMESTIC, OVERSEAS';
COMMENT ON COLUMN tb_stock_holding.market_scope_group IS '계좌 공통 코드 그룹키: MARKET_SCOPE';
COMMENT ON COLUMN tb_stock_holding.market_scope IS '계좌 코드: GENERAL, DOMESTIC, ISA, PENSION';
COMMENT ON COLUMN tb_stock_holding.stock_grade_group IS '종목등급 공통 코드 그룹키: STOCK_GRADE';
COMMENT ON COLUMN tb_stock_holding.stock_grade IS '종목등급 코드: CORE, SATELLITE, THEME, CASH_LIKE';

CREATE VIEW vw_account_stock AS
SELECT
    h.market_scope,
    scope_code.code_name AS market_scope_name,
    s.stock_code,
    s.stock_name,
    h.stock_grade,
    grade_code.code_name AS stock_grade_name,
    s.listing_scope,
    s.asset_type,
    s.exchange_name,
    s.currency,
    s.provider,
    s.active_yn,
    h.created_at,
    h.updated_at
FROM tb_stock_holding h
JOIN tb_stock s ON s.stock_code = h.stock_code
JOIN tb_common_code scope_code
  ON scope_code.code_group = h.market_scope_group
 AND scope_code.code_key = h.market_scope
LEFT JOIN tb_common_code grade_code
  ON grade_code.code_group = h.stock_grade_group
 AND grade_code.code_key = h.stock_grade;

COMMENT ON VIEW vw_account_stock IS '계좌, 종목 및 종목등급 통합 조회 뷰';
COMMENT ON COLUMN vw_account_stock.market_scope IS '계좌 코드';
COMMENT ON COLUMN vw_account_stock.market_scope_name IS '계좌 한글 명칭';
COMMENT ON COLUMN vw_account_stock.stock_grade IS '종목등급 코드';
COMMENT ON COLUMN vw_account_stock.stock_grade_name IS '종목등급 한글 명칭';
COMMENT ON COLUMN vw_account_stock.listing_scope IS '국내/해외 상장 구분';
