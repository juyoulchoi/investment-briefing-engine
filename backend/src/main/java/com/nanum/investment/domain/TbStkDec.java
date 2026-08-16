package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "\"TB_STK_DEC\"",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_TB_STK_DEC_01",
            columnNames = {"INV_DEC_ID", "ACCT_ID", "STK_ID"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbStkDec {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "STK_DEC_ID")
  private Long stockDecisionId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "INV_DEC_ID", nullable = false)
  private TbInvDec investmentDecision;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCT_ID", nullable = false)
  private TbAcct account;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STK_ID", nullable = false)
  private TbStk stock;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "HOLD_ID")
  private TbHold holding;

  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;

  @Column(name = "PL_RT", precision = 10, scale = 4)
  private BigDecimal profitLossRate;

  @Column(name = "STK_DD_RT", precision = 10, scale = 4)
  private BigDecimal stockDrawdownRate;

  @Column(name = "MKT_DD_RT", precision = 10, scale = 4)
  private BigDecimal marketDrawdownRate;

  @Column(name = "REL_PERF_RT", precision = 10, scale = 4)
  private BigDecimal relativePerformanceRate;

  @Enumerated(EnumType.STRING)
  @Column(name = "WGT_STS", length = 20)
  private WeightStatus weightStatus;

  @Column(name = "RISK_SCR", nullable = false, precision = 10, scale = 4)
  private BigDecimal riskScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "RISK_GRADE", nullable = false, length = 20)
  private RiskGrade riskGrade;

  @Column(name = "FINAL_MULT", precision = 8, scale = 4)
  private BigDecimal finalMultiplier;

  @Column(name = "REG_BUY_AMT", precision = 20, scale = 4)
  private BigDecimal regularBuyAmount;

  @Column(name = "SAVED_AMT", precision = 20, scale = 4)
  private BigDecimal savedAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "ACT_SIG", nullable = false, length = 30)
  private ActionSignal actionSignal;

  @Builder.Default
  @Column(name = "EXEC_YN", nullable = false, length = 1)
  private String executeYn = "N";

  @Column(name = "PRIO_NO")
  private Integer priorityNumber;

  @Column(name = "CONF_RT", nullable = false, precision = 10, scale = 4)
  private BigDecimal confidenceRate;

  @Column(name = "DEC_RSN", length = 2000)
  private String decisionReason;
}
