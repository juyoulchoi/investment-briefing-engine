DROP VIEW IF EXISTS vw_account_stock;

ALTER TABLE tb_overseas_stock_daily_price
    DROP CONSTRAINT fk_overseas_daily_unified_stock;

CREATE TABLE tb_stock_new (
    market_scope_group VARCHAR(50) NOT NULL DEFAULT 'MARKET_SCOPE',
    market_scope       VARCHAR(30) NOT NULL,
    stock_code         VARCHAR(30) NOT NULL,
    stock_name         VARCHAR(200) NOT NULL,
    stock_grade_group  VARCHAR(50) NOT NULL DEFAULT 'STOCK_GRADE',
    stock_grade        VARCHAR(30),
    listing_scope_group VARCHAR(50) NOT NULL DEFAULT 'LISTING_SCOPE',
    listing_scope      VARCHAR(20) NOT NULL,
    asset_type         VARCHAR(20) NOT NULL,
    exchange_name      VARCHAR(100),
    currency           VARCHAR(10) NOT NULL,
    provider           VARCHAR(50) NOT NULL,
    active_yn          CHAR(1) NOT NULL DEFAULT 'Y',
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_stock PRIMARY KEY (market_scope, stock_code),
    CONSTRAINT ck_stock_market_scope_group CHECK (market_scope_group = 'MARKET_SCOPE'),
    CONSTRAINT ck_stock_grade_group CHECK (stock_grade_group = 'STOCK_GRADE'),
    CONSTRAINT ck_stock_listing_scope_group CHECK (listing_scope_group = 'LISTING_SCOPE'),
    CONSTRAINT ck_stock_listing_scope CHECK (listing_scope IN ('DOMESTIC', 'OVERSEAS')),
    CONSTRAINT ck_stock_active_yn CHECK (active_yn IN ('Y', 'N')),
    CONSTRAINT fk_stock_market_scope_code
        FOREIGN KEY (market_scope_group, market_scope)
        REFERENCES tb_common_code (code_group, code_key),
    CONSTRAINT fk_stock_grade_code
        FOREIGN KEY (stock_grade_group, stock_grade)
        REFERENCES tb_common_code (code_group, code_key),
    CONSTRAINT fk_stock_listing_scope_code
        FOREIGN KEY (listing_scope_group, listing_scope)
        REFERENCES tb_common_code (code_group, code_key)
);

INSERT INTO tb_stock_new (
    market_scope_group, market_scope, stock_code, stock_name,
    stock_grade_group, stock_grade,
    listing_scope_group, listing_scope, asset_type, exchange_name,
    currency, provider, active_yn, created_at, updated_at
)
SELECT
    h.market_scope_group, h.market_scope, s.stock_code, s.stock_name,
    h.stock_grade_group, h.stock_grade,
    s.listing_scope_group, s.listing_scope, s.asset_type, s.exchange_name,
    s.currency, s.provider, s.active_yn, h.created_at,
    GREATEST(s.updated_at, h.updated_at)
FROM tb_stock_holding h
JOIN tb_stock s ON s.stock_code = h.stock_code;

DROP TABLE tb_stock_holding;
DROP TABLE tb_stock;
ALTER TABLE tb_stock_new RENAME TO tb_stock;

ALTER TABLE tb_overseas_stock_daily_price
    ADD COLUMN market_scope VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    ADD CONSTRAINT ck_overseas_daily_market_scope
        CHECK (market_scope = 'GENERAL'),
    ADD CONSTRAINT fk_overseas_daily_stock
        FOREIGN KEY (market_scope, symbol)
        REFERENCES tb_stock (market_scope, stock_code)
        ON DELETE CASCADE;

COMMENT ON TABLE tb_stock IS '계좌별 국내외 보유 종목 통합 테이블';
COMMENT ON COLUMN tb_stock.market_scope_group IS '계좌 공통 코드 그룹키: MARKET_SCOPE';
COMMENT ON COLUMN tb_stock.market_scope IS '계좌 코드: GENERAL, DOMESTIC, ISA, PENSION';
COMMENT ON COLUMN tb_stock.stock_code IS '국내 종목코드 또는 해외주식 심볼';
COMMENT ON COLUMN tb_stock.stock_name IS '종목명';
COMMENT ON COLUMN tb_stock.stock_grade_group IS '종목등급 공통 코드 그룹키: STOCK_GRADE';
COMMENT ON COLUMN tb_stock.stock_grade IS '종목등급 코드: CORE, SATELLITE, THEME, CASH_LIKE';
COMMENT ON COLUMN tb_stock.listing_scope_group IS '상장시장 공통 코드 그룹키: LISTING_SCOPE';
COMMENT ON COLUMN tb_stock.listing_scope IS '상장시장 코드: DOMESTIC, OVERSEAS';
COMMENT ON COLUMN tb_stock.asset_type IS '자산 유형: STOCK, ETF 등';
COMMENT ON COLUMN tb_stock.exchange_name IS '종목 상장 거래소';
COMMENT ON COLUMN tb_stock.currency IS '거래 통화';
COMMENT ON COLUMN tb_stock.provider IS '종목 및 시세 데이터 제공처';
COMMENT ON COLUMN tb_stock.active_yn IS '종목 사용 여부: Y 또는 N';
COMMENT ON COLUMN tb_stock.created_at IS '계좌별 종목 등록 일시';
COMMENT ON COLUMN tb_stock.updated_at IS '계좌별 종목 최종 수정 일시';
COMMENT ON COLUMN tb_overseas_stock_daily_price.market_scope IS '해외주식 보유 계좌 코드: GENERAL';

CREATE VIEW vw_account_stock AS
SELECT
    s.market_scope,
    scope_code.code_name AS market_scope_name,
    s.stock_code,
    s.stock_name,
    s.stock_grade,
    grade_code.code_name AS stock_grade_name,
    s.listing_scope,
    s.asset_type,
    s.exchange_name,
    s.currency,
    s.provider,
    s.active_yn,
    s.created_at,
    s.updated_at
FROM tb_stock s
JOIN tb_common_code scope_code
  ON scope_code.code_group = s.market_scope_group
 AND scope_code.code_key = s.market_scope
LEFT JOIN tb_common_code grade_code
  ON grade_code.code_group = s.stock_grade_group
 AND grade_code.code_key = s.stock_grade;

COMMENT ON VIEW vw_account_stock IS '계좌별 보유 종목 및 종목등급 조회 뷰';
