package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "\"TB_ADD_BUY\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbAddBuy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ADD_BUY_ID")
  private Long additionalBuyId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STK_DEC_ID", nullable = false, unique = true)
  private TbStkDec stockDecision;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCT_ID", nullable = false)
  private TbAcct account;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STK_ID", nullable = false)
  private TbStk stock;

  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;

  @Column(name = "ELIG_YN", nullable = false, length = 1)
  private String eligibleYn;

  @Column(name = "PRIO_NO")
  private Integer priorityNumber;

  @Column(name = "PRIO_SCR", precision = 10, scale = 4)
  private BigDecimal priorityScore;

  @Column(name = "RCMD_ADD_AMT", precision = 20, scale = 4)
  private BigDecimal recommendedAdditionalBuyAmount;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "CASH_TP", nullable = false, length = 20)
  private CashType cashType = CashType.RESERVE;

  @Builder.Default
  @Column(name = "EXEC_YN", nullable = false, length = 1)
  private String executeYn = "N";
}
