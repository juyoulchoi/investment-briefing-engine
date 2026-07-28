CREATE TABLE IF NOT EXISTS TB_KRX_DAILY_PRICE (
 id BIGSERIAL PRIMARY KEY,
 market_type VARCHAR(10) NOT NULL,
 trade_date DATE NOT NULL,
 standard_code VARCHAR(12),
 short_code VARCHAR(6) NOT NULL,
 stock_name VARCHAR(200) NOT NULL,
 close_price NUMERIC(20,4), change_amount NUMERIC(20,4), change_rate NUMERIC(12,6),
 open_price NUMERIC(20,4), high_price NUMERIC(20,4), low_price NUMERIC(20,4),
 volume BIGINT, trading_value NUMERIC(30,4), market_cap NUMERIC(30,4), listed_shares BIGINT,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT UK_KRX_DAILY_PRICE UNIQUE(market_type,trade_date,short_code)
);
CREATE INDEX IF NOT EXISTS IX_KRX_PRICE_CODE_DATE ON TB_KRX_DAILY_PRICE(short_code,trade_date DESC);
