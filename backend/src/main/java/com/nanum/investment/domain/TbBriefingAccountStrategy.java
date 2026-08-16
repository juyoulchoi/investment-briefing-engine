package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_brf_acct_strg")
@Getter
@Setter
@NoArgsConstructor
public class TbBriefingAccountStrategy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "account_strategy_id")
  private Long accountStrategyId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "briefing_id", nullable = false)
  private TbInvestmentBriefing briefing;

  @Column(name = "account_type_group", nullable = false, length = 50)
  private String accountTypeGroup = "ACCOUNT_TYPE";

  @Column(name = "account_type", nullable = false, length = 30)
  private String accountType;

  @Column(name = "market_signal", length = 30)
  private String marketSignal;

  @Column(name = "regular_buy_signal", length = 30)
  private String regularBuySignal;

  @Column(name = "additional_buy_signal", length = 30)
  private String additionalBuySignal;

  @Column(name = "cash_strategy", length = 30)
  private String cashStrategy;

  @Column(name = "invest_amount", precision = 18, scale = 2)
  private BigDecimal investAmount;

  @Column(name = "cash_balance", precision = 18, scale = 2)
  private BigDecimal cashBalance;

  @Column(name = "cash_ratio", precision = 10, scale = 4)
  private BigDecimal cashRatio;

  @Column(name = "strategy_summary", columnDefinition = "TEXT")
  private String strategySummary;

  @Column(name = "caution_message", columnDefinition = "TEXT")
  private String cautionMessage;
}
