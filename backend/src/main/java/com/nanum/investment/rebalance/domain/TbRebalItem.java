package com.nanum.investment.rebalance.domain;

import com.nanum.investment.briefing.domain.ExecutionType;
import com.nanum.investment.briefing.domain.TbStkDec;
import com.nanum.investment.common.domain.TbAcct;
import com.nanum.investment.common.domain.TbStk;
import com.nanum.investment.holding.domain.TbHold;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "\"TB_REBAL_ITEM\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbRebalItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "REBAL_ITEM_ID")
  private Long rebalanceItemId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "REBAL_ID", nullable = false)
  private TbRebal rebalance;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCT_ID", nullable = false)
  private TbAcct account;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STK_ID", nullable = false)
  private TbStk stock;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "HOLD_ID")
  private TbHold holding;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STK_DEC_ID")
  private TbStkDec stockDecision;

  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;

  @Column(name = "CUR_PRC", precision = 20, scale = 6)
  private BigDecimal currentPrice;

  @Builder.Default
  @Column(name = "HOLD_QTY", nullable = false, precision = 20, scale = 8)
  private BigDecimal holdingQuantity = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "CUR_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal currentAmount = BigDecimal.ZERO;

  @Column(name = "CUR_WGT", precision = 10, scale = 4)
  private BigDecimal currentWeight;

  @Column(name = "TGT_WGT", precision = 10, scale = 4)
  private BigDecimal targetWeight;

  @Column(name = "MIN_WGT", precision = 10, scale = 4)
  private BigDecimal minimumWeight;

  @Column(name = "MAX_WGT", precision = 10, scale = 4)
  private BigDecimal maximumWeight;

  @Column(name = "TGT_AMT", precision = 20, scale = 4)
  private BigDecimal targetAmount;

  @Column(name = "WGT_GAP_AMT", precision = 20, scale = 4)
  private BigDecimal weightGapAmount;

  @Column(name = "WGT_DIFF_RT", precision = 10, scale = 4)
  private BigDecimal weightDifferenceRate;

  @Enumerated(EnumType.STRING)
  @Column(name = "WGT_STS", nullable = false, length = 20)
  private RebalanceWeightStatus weightStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "REBAL_ACT", nullable = false, length = 30)
  private RebalanceAction rebalanceAction;

  @Builder.Default
  @Column(name = "BUY_NEED_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal buyNeedAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "SELL_NEED_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal sellNeedAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "RCMD_BUY_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal recommendedBuyAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "RCMD_SELL_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal recommendedSellAmount = BigDecimal.ZERO;

  @Column(name = "CUR_REG_BUY_AMT", precision = 20, scale = 4)
  private BigDecimal currentRegularBuyAmount;

  @Column(name = "NEW_REG_BUY_AMT", precision = 20, scale = 4)
  private BigDecimal newRegularBuyAmount;

  @Column(name = "REG_BUY_CHG_AMT", precision = 20, scale = 4)
  private BigDecimal regularBuyChangeAmount;

  @Column(name = "PRIO_NO")
  private Integer priorityNumber;

  @Column(name = "PRIO_SCR", precision = 10, scale = 4)
  private BigDecimal priorityScore;

  @Column(name = "RISK_GRADE", length = 20)
  private String riskGrade;

  @Builder.Default
  @Column(name = "FUND_DMG_YN", nullable = false, length = 1)
  private String fundamentalDamageYn = "N";

  @Builder.Default
  @Column(name = "TAX_CONSIDER_YN", nullable = false, length = 1)
  private String taxConsiderYn = "N";

  @Builder.Default
  @Column(name = "TRADE_LIMIT_YN", nullable = false, length = 1)
  private String tradeLimitYn = "N";

  @Builder.Default
  @Column(name = "EXEC_YN", nullable = false, length = 1)
  private String executeYn = "N";

  @Enumerated(EnumType.STRING)
  @Column(name = "EXEC_TP", length = 20)
  private ExecutionType executionType;

  @Builder.Default
  @Column(name = "EXEC_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal executionAmount = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "ITEM_STS", nullable = false, length = 30)
  private RebalanceItemStatus itemStatus = RebalanceItemStatus.READY;

  @Column(name = "DEC_RSN", length = 2000)
  private String decisionReason;
}
