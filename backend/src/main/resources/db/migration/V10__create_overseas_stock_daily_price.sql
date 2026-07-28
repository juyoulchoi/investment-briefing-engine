CREATE TABLE IF NOT EXISTS tb_overseas_stock_daily_price (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(30) NOT NULL,
    trading_day DATE NOT NULL,
    open_price NUMERIC(24, 8),
    high_price NUMERIC(24, 8),
    low_price NUMERIC(24, 8),
    close_price NUMERIC(24, 8) NOT NULL,
    adjusted_close NUMERIC(24, 8),
    volume BIGINT,
    provider VARCHAR(30) NOT NULL DEFAULT 'YAHOO_FINANCE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_overseas_daily_symbol_date UNIQUE(symbol, trading_day)
);
CREATE INDEX IF NOT EXISTS ix_overseas_daily_symbol_date
    ON tb_overseas_stock_daily_price(symbol, trading_day DESC);
COMMENT ON TABLE tb_overseas_stock_daily_price IS '보유 해외주식 Yahoo Finance 일별 OHLCV';
