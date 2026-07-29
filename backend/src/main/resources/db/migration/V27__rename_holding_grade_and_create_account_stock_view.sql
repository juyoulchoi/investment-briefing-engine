ALTER TABLE tb_stock_holding
    RENAME COLUMN portfolio_role_group TO stock_grade_group;

ALTER TABLE tb_stock_holding
    RENAME COLUMN portfolio_role TO stock_grade;

COMMENT ON COLUMN tb_stock_holding.stock_grade_group IS '종목등급 공통 코드 그룹키: PORTFOLIO_ROLE';
COMMENT ON COLUMN tb_stock_holding.stock_grade IS '계좌별 종목등급 코드: CORE, SATELLITE, THEME, CASH_LIKE';

CREATE VIEW vw_account_stock AS
SELECT
    h.account_type AS market_scope,
    account_code.code_name AS account_name,
    s.stock_code,
    s.stock_name,
    h.stock_grade,
    grade_code.code_name AS stock_grade_name,
    s.asset_type,
    s.exchange_name,
    s.currency,
    s.provider,
    s.active_yn,
    h.created_at,
    h.updated_at
FROM tb_stock_holding h
JOIN tb_stock s
  ON s.stock_code = h.stock_code
JOIN tb_common_code account_code
  ON account_code.code_group = h.account_type_group
 AND account_code.code_key = h.account_type
LEFT JOIN tb_common_code grade_code
  ON grade_code.code_group = h.stock_grade_group
 AND grade_code.code_key = h.stock_grade;

COMMENT ON VIEW vw_account_stock IS '계좌, 종목코드, 종목명, 종목등급 기준 통합 보유종목';
COMMENT ON COLUMN vw_account_stock.market_scope IS '보유 계좌 코드: GENERAL, DOMESTIC, ISA, PENSION';
COMMENT ON COLUMN vw_account_stock.account_name IS '보유 계좌 한글 명칭';
COMMENT ON COLUMN vw_account_stock.stock_code IS '국내외 통합 종목코드';
COMMENT ON COLUMN vw_account_stock.stock_name IS '국내외 통합 종목명';
COMMENT ON COLUMN vw_account_stock.stock_grade IS '종목등급 코드';
COMMENT ON COLUMN vw_account_stock.stock_grade_name IS '종목등급 한글 명칭: 핵심, 위성, 테마, 현금성';
