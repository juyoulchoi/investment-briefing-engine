CREATE TABLE tb_daily_market_data (
    trade_date       DATE NOT NULL,
    indicator_code   VARCHAR(50) NOT NULL,
    indicator_name   VARCHAR(100),
    close_value      NUMERIC(20, 6),
    change_value     NUMERIC(20, 6),
    change_rate      NUMERIC(10, 4),
    source_name      VARCHAR(100),
    collected_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (trade_date, indicator_code)
);

COMMENT ON TABLE tb_daily_market_data IS '거래일별 시장 지표 수집 데이터';
COMMENT ON COLUMN tb_daily_market_data.trade_date IS '시장 지표 거래 기준일';
COMMENT ON COLUMN tb_daily_market_data.indicator_code IS '시장 지표 고유 코드';
COMMENT ON COLUMN tb_daily_market_data.indicator_name IS '시장 지표 명칭';
COMMENT ON COLUMN tb_daily_market_data.close_value IS '시장 지표 종가 또는 최종 값';
COMMENT ON COLUMN tb_daily_market_data.change_value IS '전일 대비 증감 값';
COMMENT ON COLUMN tb_daily_market_data.change_rate IS '전일 대비 등락률';
COMMENT ON COLUMN tb_daily_market_data.source_name IS '시장 데이터 제공처';
COMMENT ON COLUMN tb_daily_market_data.collected_at IS '데이터 수집 일시';
