CREATE TABLE IF NOT EXISTS tb_overseas_stock_quote (
    symbol VARCHAR(30) PRIMARY KEY,
    trading_day DATE NOT NULL,
    open_price NUMERIC(24, 8),
    high_price NUMERIC(24, 8),
    low_price NUMERIC(24, 8),
    price NUMERIC(24, 8) NOT NULL,
    previous_close NUMERIC(24, 8),
    change_amount NUMERIC(24, 8),
    change_percent VARCHAR(30),
    volume BIGINT,
    provider VARCHAR(30) NOT NULL,
    payload JSONB NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_overseas_quote_updated ON tb_overseas_stock_quote(updated_at DESC);
COMMENT ON TABLE tb_overseas_stock_quote IS '해외주식 종목별 최신 시세';
