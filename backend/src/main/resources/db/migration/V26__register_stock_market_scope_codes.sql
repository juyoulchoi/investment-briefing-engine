INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
VALUES
    ('STOCK_MARKET_SCOPE', 'DOMESTIC', '국내',   '국내 거래소에 상장된 주식 또는 ETF', 1, 'Y'),
    ('STOCK_MARKET_SCOPE', 'OVERSEAS', '해외',   '해외 거래소에 상장된 주식 또는 ETF', 2, 'Y'),
    ('STOCK_MARKET_SCOPE', 'ISA',      'ISA',    'ISA 계좌에서 관리하는 투자 종목 범위', 3, 'Y'),
    ('STOCK_MARKET_SCOPE', 'PENSION',  '연금',   '연금계좌에서 관리하는 투자 종목 범위', 4, 'Y');

ALTER TABLE tb_stock
    DROP CONSTRAINT ck_stock_market_scope,
    ADD COLUMN market_scope_group VARCHAR(50) NOT NULL DEFAULT 'STOCK_MARKET_SCOPE',
    ADD CONSTRAINT ck_stock_market_scope_group
        CHECK (market_scope_group = 'STOCK_MARKET_SCOPE'),
    ADD CONSTRAINT ck_stock_market_scope
        CHECK (market_scope IN ('DOMESTIC', 'OVERSEAS', 'ISA', 'PENSION')),
    ADD CONSTRAINT fk_stock_market_scope_common_code
        FOREIGN KEY (market_scope_group, market_scope)
        REFERENCES tb_common_code (code_group, code_key);

COMMENT ON COLUMN tb_stock.market_scope_group IS '종목 관리 범위 공통 코드 그룹키: STOCK_MARKET_SCOPE';
COMMENT ON COLUMN tb_stock.market_scope IS '종목 관리 범위 코드: DOMESTIC, OVERSEAS, ISA, PENSION';
