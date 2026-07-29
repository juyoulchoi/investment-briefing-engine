DROP VIEW IF EXISTS vw_account_stock;

ALTER TABLE tb_stock
    DROP CONSTRAINT fk_stock_market_scope_code,
    DROP CONSTRAINT fk_stock_grade_code,
    DROP CONSTRAINT fk_stock_listing_scope_code,
    DROP CONSTRAINT ck_stock_market_scope_group,
    DROP CONSTRAINT ck_stock_grade_group,
    DROP CONSTRAINT ck_stock_listing_scope_group;

ALTER TABLE tb_stock
    DROP COLUMN market_scope_group,
    DROP COLUMN stock_grade_group,
    DROP COLUMN listing_scope_group;

ALTER TABLE tb_stock
    ADD CONSTRAINT ck_stock_market_scope
        CHECK (market_scope IN ('GENERAL', 'DOMESTIC', 'ISA', 'PENSION')),
    ADD CONSTRAINT ck_stock_grade
        CHECK (stock_grade IS NULL OR stock_grade IN ('CORE', 'SATELLITE', 'THEME', 'CASH_LIKE'));

COMMENT ON COLUMN tb_stock.market_scope IS
    '계좌 코드. 공통코드 MARKET_SCOPE 그룹과 논리적으로 연결: GENERAL, DOMESTIC, ISA, PENSION';
COMMENT ON COLUMN tb_stock.stock_grade IS
    '종목등급 코드. 공통코드 STOCK_GRADE 그룹과 논리적으로 연결: CORE, SATELLITE, THEME, CASH_LIKE';
COMMENT ON COLUMN tb_stock.listing_scope IS
    '상장시장 코드. 공통코드 LISTING_SCOPE 그룹과 논리적으로 연결: DOMESTIC, OVERSEAS';

CREATE VIEW vw_account_stock AS
SELECT
    s.market_scope,
    scope_code.code_name AS market_scope_name,
    s.stock_code,
    s.stock_name,
    s.stock_grade,
    grade_code.code_name AS stock_grade_name,
    s.listing_scope,
    listing_code.code_name AS listing_scope_name,
    s.asset_type,
    s.exchange_name,
    s.currency,
    s.provider,
    s.active_yn,
    s.created_at,
    s.updated_at
FROM tb_stock s
JOIN tb_common_code scope_code
  ON scope_code.code_group = 'MARKET_SCOPE'
 AND scope_code.code_key = s.market_scope
LEFT JOIN tb_common_code grade_code
  ON grade_code.code_group = 'STOCK_GRADE'
 AND grade_code.code_key = s.stock_grade
JOIN tb_common_code listing_code
  ON listing_code.code_group = 'LISTING_SCOPE'
 AND listing_code.code_key = s.listing_scope;

COMMENT ON VIEW vw_account_stock IS '계좌별 보유 종목 및 종목등급 조회 뷰';
COMMENT ON COLUMN vw_account_stock.market_scope IS '계좌 코드';
COMMENT ON COLUMN vw_account_stock.market_scope_name IS '계좌 한글 명칭';
COMMENT ON COLUMN vw_account_stock.stock_grade IS '종목등급 코드';
COMMENT ON COLUMN vw_account_stock.stock_grade_name IS '종목등급 한글 명칭';
COMMENT ON COLUMN vw_account_stock.listing_scope IS '상장시장 코드';
COMMENT ON COLUMN vw_account_stock.listing_scope_name IS '상장시장 한글 명칭';
