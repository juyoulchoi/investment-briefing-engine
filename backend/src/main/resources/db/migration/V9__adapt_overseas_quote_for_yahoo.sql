ALTER TABLE tb_overseas_stock_quote
    ADD COLUMN IF NOT EXISTS company_name VARCHAR(300),
    ADD COLUMN IF NOT EXISTS exchange_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10);

UPDATE tb_overseas_stock_quote SET provider = 'YAHOO_FINANCE' WHERE provider = 'ALPHA_VANTAGE';
COMMENT ON COLUMN tb_overseas_stock_quote.company_name IS 'Yahoo Finance 종목명';
COMMENT ON COLUMN tb_overseas_stock_quote.exchange_name IS 'Yahoo Finance 거래소명';
COMMENT ON COLUMN tb_overseas_stock_quote.currency IS '거래 통화';
