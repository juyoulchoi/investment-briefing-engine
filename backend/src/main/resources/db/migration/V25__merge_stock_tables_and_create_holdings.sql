INSERT INTO tb_common_code
    (code_group, code_key, code_name, description, display_order, active_yn)
VALUES
    ('PORTFOLIO_ROLE', 'CORE',      '핵심',   '장기 보유와 포트폴리오 중심 역할을 하는 종목', 1, 'Y'),
    ('PORTFOLIO_ROLE', 'SATELLITE', '위성',   '핵심 자산을 보완하고 추가 수익을 추구하는 종목', 2, 'Y'),
    ('PORTFOLIO_ROLE', 'THEME',     '테마',   '특정 산업 또는 투자 테마에 집중하는 종목', 3, 'Y'),
    ('PORTFOLIO_ROLE', 'CASH_LIKE', '현금성', '단기 자금 보관과 유동성 확보 목적의 자산', 4, 'Y');

CREATE TABLE tb_stock (
    stock_code    VARCHAR(30) PRIMARY KEY,
    stock_name    VARCHAR(200) NOT NULL,
    market_scope  VARCHAR(20) NOT NULL,
    asset_type    VARCHAR(20) NOT NULL,
    exchange_name VARCHAR(100),
    currency      VARCHAR(10) NOT NULL,
    provider      VARCHAR(50) NOT NULL,
    active_yn     CHAR(1) NOT NULL DEFAULT 'Y',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_stock_market_scope CHECK (market_scope IN ('DOMESTIC', 'OVERSEAS')),
    CONSTRAINT ck_stock_active_yn CHECK (active_yn IN ('Y', 'N'))
);

COMMENT ON TABLE tb_stock IS '국내주식과 해외주식 통합 종목 기본정보';
COMMENT ON COLUMN tb_stock.stock_code IS '국내 종목코드 또는 해외주식 심볼';
COMMENT ON COLUMN tb_stock.stock_name IS '통합 종목명';
COMMENT ON COLUMN tb_stock.market_scope IS '시장 범위: DOMESTIC 또는 OVERSEAS';
COMMENT ON COLUMN tb_stock.asset_type IS '자산 유형: STOCK, ETF 등';
COMMENT ON COLUMN tb_stock.exchange_name IS '종목 상장 거래소';
COMMENT ON COLUMN tb_stock.currency IS '거래 통화';
COMMENT ON COLUMN tb_stock.provider IS '종목 및 시세 데이터 제공처';
COMMENT ON COLUMN tb_stock.active_yn IS '종목 사용 여부: Y 또는 N';
COMMENT ON COLUMN tb_stock.created_at IS '종목 등록 일시';
COMMENT ON COLUMN tb_stock.updated_at IS '종목 최종 수정 일시';

INSERT INTO tb_stock
    (stock_code, stock_name, market_scope, asset_type, currency, provider, active_yn, created_at, updated_at)
SELECT stock_code, stock_name, 'DOMESTIC', asset_type, 'KRW', 'KRX',
       active_yn, created_at, updated_at
FROM tb_domestic_stock;

INSERT INTO tb_stock
    (stock_code, stock_name, market_scope, asset_type, exchange_name, currency,
     provider, active_yn, created_at, updated_at)
SELECT symbol, company_name, 'OVERSEAS',
       CASE WHEN company_name ILIKE '%ETF%' OR company_name ILIKE '%Fund%' OR company_name ILIKE '%Trust%'
            THEN 'ETF' ELSE 'STOCK' END,
       exchange_name, currency, provider, 'Y', created_at, updated_at
FROM tb_overseas_stock
ON CONFLICT (stock_code) DO UPDATE SET
    stock_name=EXCLUDED.stock_name,
    exchange_name=EXCLUDED.exchange_name,
    currency=EXCLUDED.currency,
    provider=EXCLUDED.provider,
    updated_at=EXCLUDED.updated_at;

CREATE TABLE tb_stock_holding (
    account_type_group  VARCHAR(50) NOT NULL DEFAULT 'ACCOUNT_TYPE',
    account_type        VARCHAR(30) NOT NULL,
    stock_code          VARCHAR(30) NOT NULL,
    portfolio_role_group VARCHAR(50) NOT NULL DEFAULT 'PORTFOLIO_ROLE',
    portfolio_role      VARCHAR(30),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (account_type, stock_code),
    CONSTRAINT ck_stock_holding_account_group CHECK (account_type_group = 'ACCOUNT_TYPE'),
    CONSTRAINT ck_stock_holding_role_group CHECK (portfolio_role_group = 'PORTFOLIO_ROLE'),
    CONSTRAINT fk_stock_holding_account_code
        FOREIGN KEY (account_type_group, account_type)
        REFERENCES tb_common_code (code_group, code_key),
    CONSTRAINT fk_stock_holding_role_code
        FOREIGN KEY (portfolio_role_group, portfolio_role)
        REFERENCES tb_common_code (code_group, code_key),
    CONSTRAINT fk_stock_holding_stock
        FOREIGN KEY (stock_code)
        REFERENCES tb_stock (stock_code)
        ON DELETE CASCADE
);

COMMENT ON TABLE tb_stock_holding IS '계좌별 국내외 보유 종목과 포트폴리오 역할';
COMMENT ON COLUMN tb_stock_holding.account_type_group IS '계좌 유형 공통 코드 그룹키: ACCOUNT_TYPE';
COMMENT ON COLUMN tb_stock_holding.account_type IS '보유 계좌 유형 코드';
COMMENT ON COLUMN tb_stock_holding.stock_code IS '통합 종목코드';
COMMENT ON COLUMN tb_stock_holding.portfolio_role_group IS '포트폴리오 역할 공통 코드 그룹키: PORTFOLIO_ROLE';
COMMENT ON COLUMN tb_stock_holding.portfolio_role IS '포트폴리오 역할 코드: CORE, SATELLITE, THEME, CASH_LIKE';
COMMENT ON COLUMN tb_stock_holding.created_at IS '보유 종목 등록 일시';
COMMENT ON COLUMN tb_stock_holding.updated_at IS '보유 종목 최종 수정 일시';

INSERT INTO tb_stock_holding (account_type, stock_code, portfolio_role)
SELECT 'GENERAL', symbol, NULL FROM tb_overseas_stock;

INSERT INTO tb_stock_holding (account_type, stock_code, portfolio_role)
VALUES
    ('DOMESTIC','007340','SATELLITE'), ('DOMESTIC','105560','SATELLITE'),
    ('DOMESTIC','010120','SATELLITE'), ('DOMESTIC','001440','THEME'),
    ('DOMESTIC','034020','SATELLITE'), ('DOMESTIC','058470','SATELLITE'),
    ('DOMESTIC','083650','SATELLITE'), ('DOMESTIC','028050','SATELLITE'),
    ('DOMESTIC','005930','CORE'), ('DOMESTIC','032820','THEME'),
    ('DOMESTIC','000100','SATELLITE'), ('DOMESTIC','014680','SATELLITE'),
    ('DOMESTIC','000720','SATELLITE'), ('DOMESTIC','000660','CORE'),
    ('DOMESTIC','298040','CORE'),
    ('ISA','411060','CORE'), ('ISA','069500','CORE'), ('ISA','305720','THEME'),
    ('ISA','471990','SATELLITE'), ('ISA','117700','SATELLITE'),
    ('ISA','487230','SATELLITE'), ('ISA','379800','CORE'),
    ('ISA','144600','SATELLITE'), ('ISA','266420','SATELLITE'),
    ('ISA','161510','CORE'), ('ISA','0023A0','THEME'),
    ('ISA','139270','SATELLITE'), ('ISA','227550','SATELLITE'),
    ('ISA','160580','SATELLITE'), ('ISA','464310','SATELLITE'),
    ('ISA','329200','SATELLITE'), ('ISA','458730','CORE'),
    ('ISA','0183J0','SATELLITE'), ('ISA','305080','CORE'),
    ('ISA','0046A0','CORE'), ('ISA','381180','SATELLITE'),
    ('ISA','466940','CORE'), ('ISA','494670','SATELLITE'),
    ('ISA','302190','CORE'),
    ('PENSION','411060','CORE'), ('PENSION','069500','CORE'),
    ('PENSION','305720','THEME'), ('PENSION','471990','SATELLITE'),
    ('PENSION','117700','SATELLITE'), ('PENSION','0089D0','CORE'),
    ('PENSION','379800','CORE'), ('PENSION','144600','SATELLITE'),
    ('PENSION','266420','SATELLITE'), ('PENSION','161510','CORE'),
    ('PENSION','455890','CASH_LIKE'), ('PENSION','0023A0','THEME'),
    ('PENSION','0051G0','SATELLITE'), ('PENSION','139270','SATELLITE'),
    ('PENSION','227550','SATELLITE'), ('PENSION','463250','SATELLITE'),
    ('PENSION','160580','SATELLITE'), ('PENSION','464310','SATELLITE'),
    ('PENSION','329200','SATELLITE'), ('PENSION','458730','CORE'),
    ('PENSION','0183J0','SATELLITE'), ('PENSION','305080','CORE'),
    ('PENSION','0046A0','CORE'), ('PENSION','381180','SATELLITE'),
    ('PENSION','241180','SATELLITE'), ('PENSION','494670','SATELLITE'),
    ('PENSION','302190','CORE');

ALTER TABLE tb_overseas_stock_daily_price
    DROP CONSTRAINT fk_overseas_daily_stock,
    ADD CONSTRAINT fk_overseas_daily_unified_stock
        FOREIGN KEY (symbol)
        REFERENCES tb_stock (stock_code)
        ON DELETE CASCADE;

DROP TABLE tb_domestic_stock;
DROP TABLE tb_overseas_stock;
