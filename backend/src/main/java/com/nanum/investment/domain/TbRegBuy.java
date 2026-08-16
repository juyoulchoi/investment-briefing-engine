package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "\"TB_REG_BUY\"",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_TB_REG_BUY_01",
            columnNames = {"ACCT_ID", "STK_ID"}))
@IdClass(TbRegBuyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbRegBuy {
  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "ACCT_TP", nullable = false, length = 20)
  private AccountType accountType;

  @Id
  @Column(name = "STK_CD", nullable = false, length = 30)
  private String stockCode;

  @Column(name = "STK_NM", nullable = false)
  private String legacyStockName;

  @Column(name = "CYCLE_TP", nullable = false)
  private String legacyCycleType;

  @Column(name = "WEEK_DAY")
  private String legacyWeekDays;

  @Column(name = "MONTH_DAY")
  private Integer legacyMonthDay;

  @Column(name = "APPLIED_DAY_NOS", length = 100)
  private String appliedMonthDays;

  @Column(name = "AMT", precision = 20, scale = 4)
  private BigDecimal appliedAmount;

  @Builder.Default
  @Column(name = "ACTV_YN", nullable = false, length = 1)
  private String legacyActiveYn = "Y";

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCT_ID", nullable = false)
  private TbAcct account;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STK_ID", nullable = false)
  private TbStk stock;

  @Enumerated(EnumType.STRING)
  @Column(name = "BUY_CYCLE", nullable = false, length = 20)
  private BuyCycle buyCycle;

  @Column(name = "BUY_DAY_CD", length = 100)
  private String buyDayCode;

  @Column(name = "BUY_DAY_NO")
  private Integer buyDayNumber;

  @Column(name = "BUY_DAY_NOS", length = 100)
  private String buyDayNumbers;

  @Builder.Default
  @Column(name = "BUY_BASIS", nullable = false, length = 10)
  private String buyBasis = "AMOUNT";

  @Builder.Default
  @Column(name = "MIN_BUY_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal minimumBuyAmount = BigDecimal.ZERO;

  @Column(name = "BASE_QTY", precision = 20, scale = 8)
  private BigDecimal baseBuyQuantity;

  @Column(name = "QTY", precision = 20, scale = 8)
  private BigDecimal buyQuantity;

  @Column(name = "MKT_MULT", precision = 8, scale = 4)
  private BigDecimal marketMultiplier;

  @Column(name = "STK_ADJ_MULT", precision = 8, scale = 4)
  private BigDecimal stockAdjustmentMultiplier;

  @Column(name = "WGT_ADJ_MULT", precision = 8, scale = 4)
  private BigDecimal weightAdjustmentMultiplier;

  @Column(name = "RISK_ADJ_MULT", precision = 8, scale = 4)
  private BigDecimal riskAdjustmentMultiplier;

  @Column(name = "FINAL_MULT", precision = 8, scale = 4)
  private BigDecimal finalMultiplier;

  @Column(name = "BASE_BUY_AMT", precision = 20, scale = 4)
  private BigDecimal baseBuyAmount;

  @Column(name = "RCMD_BUY_AMT", precision = 20, scale = 4)
  private BigDecimal recommendedBuyAmount;

  @Column(name = "SAVED_AMT", precision = 20, scale = 4)
  private BigDecimal savedAmount;

  @Column(name = "ACT_SIG", length = 30)
  private String actionSignal;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "BUY_STS", nullable = false, length = 20)
  private RegularBuyStatus buyStatus = RegularBuyStatus.ACTIVE;

  @Column(name = "PRIORITY")
  private Integer priority;

  @Column(name = "INV_GRD", length = 50)
  private String investmentGrade;

  @Column(name = "MEMO", length = 1000)
  private String memo;

  @Column(name = "PAUSE_RSN", length = 500)
  private String pauseReason;

  @Builder.Default
  @Column(name = "USER_PAUSE_YN", nullable = false, length = 1)
  private String userPauseYn = "N";

  @Builder.Default
  @Column(name = "AUTO_CALC_YN", nullable = false, length = 1)
  private String autoCalculateYn = "Y";

  @Column(name = "LAST_CALC_DT")
  private LocalDate lastCalculationDate;

  @Builder.Default
  @Column(name = "RULE_VER_NO", nullable = false)
  private Integer ruleVersionNumber = 1;

  @Builder.Default
  @Column(name = "DEL_YN", nullable = false, length = 1)
  private String deleteYn = "N";
}
