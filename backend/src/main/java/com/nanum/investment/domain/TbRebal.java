package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "\"TB_REBAL\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbRebal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "REBAL_ID")
  private Long rebalanceId;

  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;

  @Builder.Default
  @Column(name = "CALC_SEQ", nullable = false)
  private Integer calculationSequence = 1;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCT_ID", nullable = false)
  private TbAcct account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INV_DEC_ID")
  private TbInvDec investmentDecision;

  @Enumerated(EnumType.STRING)
  @Column(name = "REBAL_TP", nullable = false, length = 20)
  private RebalanceType rebalanceType;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "REBAL_STS", nullable = false, length = 30)
  private RebalanceStatus rebalanceStatus = RebalanceStatus.DRAFT;

  @Column(name = "TOT_AST_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal totalAssetAmount;

  @Column(name = "CASH_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal cashAmount;

  @Column(name = "RSV_CASH_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal reservedCashAmount;

  @Column(name = "CUR_CASH_WGT", precision = 10, scale = 4)
  private BigDecimal currentCashWeight;

  @Column(name = "TGT_CASH_WGT", precision = 10, scale = 4)
  private BigDecimal targetCashWeight;

  @Column(name = "CASH_GAP_AMT", precision = 20, scale = 4)
  private BigDecimal cashGapAmount;

  @Builder.Default
  @Column(name = "NEW_CASH_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal newCashAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "BUY_BGT_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal buyBudgetAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "SELL_TGT_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal sellTargetAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "RCMD_BUY_TOT_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal recommendedBuyTotalAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "RCMD_SELL_TOT_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal recommendedSellTotalAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "EXEC_BUY_TOT_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal executedBuyTotalAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "EXEC_SELL_TOT_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal executedSellTotalAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "ITEM_CNT", nullable = false)
  private Integer itemCount = 0;

  @Builder.Default
  @Column(name = "BUY_ITEM_CNT", nullable = false)
  private Integer buyItemCount = 0;

  @Builder.Default
  @Column(name = "SELL_ITEM_CNT", nullable = false)
  private Integer sellItemCount = 0;

  @Builder.Default
  @Column(name = "HOLD_ITEM_CNT", nullable = false)
  private Integer holdItemCount = 0;

  @Builder.Default
  @Column(name = "REBAL_REQ_YN", nullable = false, length = 1)
  private String rebalanceRequiredYn = "N";

  @Builder.Default
  @Column(name = "EXEC_YN", nullable = false, length = 1)
  private String executeYn = "N";

  @Builder.Default
  @Column(name = "COMP_YN", nullable = false, length = 1)
  private String completeYn = "N";

  @Column(name = "CONF_RT", nullable = false, precision = 10, scale = 4)
  private BigDecimal confidenceRate;

  @Column(name = "KEY_RSN", length = 2000)
  private String keyReason;

  @Column(name = "EXEC_RSN", length = 1000)
  private String executionReason;

  @Builder.Default
  @Column(name = "RULE_VER_NO", nullable = false)
  private Integer ruleVersionNumber = 1;

  @Builder.Default
  @Column(name = "LATEST_YN", nullable = false, length = 1)
  private String latestYn = "Y";

  @Column(name = "CALC_DTTM", insertable = false, updatable = false)
  private OffsetDateTime calculatedDateTime;

  @Column(name = "APRV_DTTM")
  private OffsetDateTime approvedDateTime;

  @Column(name = "COMP_DTTM")
  private OffsetDateTime completedDateTime;
}
