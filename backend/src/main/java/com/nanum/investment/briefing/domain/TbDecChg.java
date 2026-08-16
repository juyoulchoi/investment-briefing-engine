package com.nanum.investment.briefing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "\"TB_DEC_CHG\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbDecChg {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "DEC_CHG_ID")
  private Long decisionChangeId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STK_DEC_ID", nullable = false)
  private TbStkDec stockDecision;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PREV_STK_DEC_ID")
  private TbStkDec previousStockDecision;

  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "CHG_TP", nullable = false, length = 30)
  private DecisionChangeType changeType;

  @Column(name = "PREV_ACT_SIG", length = 30)
  private String previousActionSignal;

  @Column(name = "CUR_ACT_SIG", length = 30)
  private String currentActionSignal;

  @Column(name = "PREV_MULT", precision = 8, scale = 4)
  private BigDecimal previousMultiplier;

  @Column(name = "CUR_MULT", precision = 8, scale = 4)
  private BigDecimal currentMultiplier;

  @Column(name = "CHG_RSN", length = 2000)
  private String changeReason;
}
