package com.nanum.investment.briefing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tb_brf_stk",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_brf_stk_01",
            columnNames = {"brf_id", "acct_tp", "stk_cd"}))
@Getter
@Setter
@NoArgsConstructor
public class TbBriefingStockSignal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "stk_sig_id")
  private Long stockSignalId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "brf_id", nullable = false)
  private TbInvestmentBriefing briefing;

  @Column(name = "acct_tp", nullable = false, length = 20)
  private String accountType;

  @Column(name = "stk_cd", nullable = false, length = 30)
  private String stockCode;

  @Column(name = "stk_nm", nullable = false, length = 100)
  private String stockName;

  @Column(name = "light_cd", length = 20)
  private String trafficLight;

  @Column(name = "act_cd", length = 30)
  private String actionSignal;

  @Column(name = "buy_st", length = 20)
  private String regularBuyStatus;

  @Column(name = "rebuy_cd", length = 20)
  private String rebuySignal;

  @Column(name = "rcm_amt", precision = 18, scale = 2)
  private BigDecimal recommendedBuyAmount;

  @Column(name = "rcm_rt", precision = 10, scale = 4)
  private BigDecimal recommendedSellRatio;

  @Column(name = "sig_rsn", columnDefinition = "TEXT")
  private String signalReason;

  @Column(columnDefinition = "TEXT")
  private String memo;

  @Column(name = "reg_dt", nullable = false, insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "mod_dt", nullable = false, insertable = false)
  private LocalDateTime updatedAt;

  @PreUpdate
  void touchUpdatedAt() {
    updatedAt = LocalDateTime.now();
  }
}
