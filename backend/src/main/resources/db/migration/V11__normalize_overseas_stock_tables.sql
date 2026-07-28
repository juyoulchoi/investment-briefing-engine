CREATE TABLE IF NOT EXISTS tb_overseas_stock (
    symbol VARCHAR(30) PRIMARY KEY,
    company_name VARCHAR(300) NOT NULL,
    exchange_name VARCHAR(100),
    currency VARCHAR(10),
    provider VARCHAR(30) NOT NULL DEFAULT 'YAHOO_FINANCE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tb_overseas_stock(symbol, company_name, exchange_name, currency, provider, updated_at)
SELECT symbol, COALESCE(NULLIF(company_name, ''), symbol), exchange_name, currency,
       'YAHOO_FINANCE', updated_at
FROM tb_overseas_stock_quote
ON CONFLICT(symbol) DO UPDATE SET
    company_name=EXCLUDED.company_name,
    exchange_name=EXCLUDED.exchange_name,
    currency=EXCLUDED.currency,
    provider=EXCLUDED.provider,
    updated_at=EXCLUDED.updated_at;

INSERT INTO tb_overseas_stock(symbol, company_name, provider)
SELECT DISTINCT symbol, symbol, 'YAHOO_FINANCE'
FROM tb_overseas_stock_daily_price
ON CONFLICT(symbol) DO NOTHING;

ALTER TABLE tb_overseas_stock_daily_price
    ADD CONSTRAINT fk_overseas_daily_stock
    FOREIGN KEY(symbol) REFERENCES tb_overseas_stock(symbol) ON DELETE CASCADE;

DROP TABLE tb_overseas_stock_quote;
COMMENT ON TABLE tb_overseas_stock IS '해외주식 종목 기본정보';
