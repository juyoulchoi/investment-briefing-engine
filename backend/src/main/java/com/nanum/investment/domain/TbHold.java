package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "\"TB_HOLD\"",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_TB_HOLD_01",
            columnNames = {"ACCT_ID", "STK_ID"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbHold {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "HOLD_ID")
  private Long holdingId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCT_ID", nullable = false)
  private TbAcct account;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STK_ID", nullable = false)
  private TbStk stock;

  @Builder.Default
  @Column(name = "HOLD_QTY", nullable = false, precision = 20, scale = 8)
  private BigDecimal holdingQuantity = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "AVG_PRC", nullable = false, precision = 20, scale = 6)
  private BigDecimal averagePrice = BigDecimal.ZERO;

  @Column(name = "CUR_PRC", precision = 20, scale = 6)
  private BigDecimal currentPrice;

  @Builder.Default
  @Column(name = "EXCH_RT", nullable = false, precision = 20, scale = 6)
  private BigDecimal exchangeRate = BigDecimal.ONE;

  @Column(name = "ORG_EVL_AMT", precision = 20, scale = 4)
  private BigDecimal originalEvaluationAmount;

  @Column(name = "EVL_AMT", precision = 20, scale = 4)
  private BigDecimal evaluationAmount;

  @Column(name = "ORG_PL_AMT", precision = 20, scale = 4)
  private BigDecimal originalProfitLossAmount;

  @Column(name = "PL_AMT", precision = 20, scale = 4)
  private BigDecimal profitLossAmount;

  @Column(name = "PL_RT", precision = 10, scale = 4)
  private BigDecimal profitLossRate;

  @Column(name = "TGT_WGT", precision = 7, scale = 4)
  private BigDecimal targetWeight;

  @Column(name = "CUR_WGT", precision = 7, scale = 4)
  private BigDecimal currentWeight;

  @Column(name = "WGT_DIFF_RT", precision = 10, scale = 4)
  private BigDecimal weightDifferenceRate;

  @Enumerated(EnumType.STRING)
  @Column(name = "WGT_STS", length = 20)
  private WeightStatus weightStatus;

  @Column(name = "RISK_GRADE", length = 20)
  private String riskGrade;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "HOLD_STS", nullable = false, length = 20)
  private HoldingStatus holdingStatus = HoldingStatus.ACTIVE;

  @Column(name = "PRC_BASE_DT")
  private LocalDate priceBaseDate;

  @Column(name = "CALC_DTTM")
  private OffsetDateTime calculatedDateTime;

  @Column(name = "MEMO", length = 1000)
  private String memo;

  @Builder.Default
  @Column(name = "USE_YN", nullable = false, length = 1)
  private String useYn = "Y";

  @Builder.Default
  @Column(name = "DEL_YN", nullable = false, length = 1)
  private String deleteYn = "N";
}
