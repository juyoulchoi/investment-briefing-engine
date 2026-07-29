CREATE TABLE tb_briefing_market_indicator (
    indicator_id           BIGSERIAL PRIMARY KEY,
    briefing_id            BIGINT NOT NULL,

    market_code            VARCHAR(30) NOT NULL,
    close_price            NUMERIC(18, 4),
    change_rate            NUMERIC(10, 4),

    foreign_net_amount     NUMERIC(20, 2),
    institution_net_amount NUMERIC(20, 2),
    individual_net_amount  NUMERIC(20, 2),

    program_net_amount     NUMERIC(20, 2),
    foreign_futures_amount NUMERIC(20, 2),

    exchange_rate          NUMERIC(12, 4),
    trading_value          NUMERIC(20, 2),

    CONSTRAINT fk_market_briefing
        FOREIGN KEY (briefing_id)
        REFERENCES tb_investment_briefing (briefing_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE tb_briefing_market_indicator IS '투자 브리핑별 시장 지표';
