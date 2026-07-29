package com.nanum.investment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_briefing_market_indicator")
@Getter
@Setter
@NoArgsConstructor
public class TbBriefingMarketIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "briefing_id", nullable = false)
    private TbInvestmentBriefing briefing;

    @Column(name = "market_code_group", nullable = false, length = 50)
    private String marketCodeGroup = "MARKET_INDICATOR";

    @Column(name = "market_code", nullable = false, length = 30)
    private String marketCode;

    @Column(name = "close_price", precision = 18, scale = 4)
    private BigDecimal closePrice;
    @Column(name = "change_rate", precision = 10, scale = 4)
    private BigDecimal changeRate;
    @Column(name = "foreign_net_amount", precision = 20, scale = 2)
    private BigDecimal foreignNetAmount;
    @Column(name = "institution_net_amount", precision = 20, scale = 2)
    private BigDecimal institutionNetAmount;
    @Column(name = "individual_net_amount", precision = 20, scale = 2)
    private BigDecimal individualNetAmount;
    @Column(name = "program_net_amount", precision = 20, scale = 2)
    private BigDecimal programNetAmount;
    @Column(name = "foreign_futures_amount", precision = 20, scale = 2)
    private BigDecimal foreignFuturesAmount;
    @Column(name = "exchange_rate", precision = 12, scale = 4)
    private BigDecimal exchangeRate;
    @Column(name = "trading_value", precision = 20, scale = 2)
    private BigDecimal tradingValue;
}
