package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "\"TB_REBUY\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbRebuy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "REBUY_ID")
  private Long rebuyId;

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

  @Column(name = "REBUY_SCR", precision = 10, scale = 4)
  private BigDecimal rebuyScore;

  @Column(name = "PRIO_NO")
  private Integer priorityNumber;

  @Column(name = "RCMD_REBUY_AMT", precision = 20, scale = 4)
  private BigDecimal recommendedRebuyAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "REBUY_SIG", nullable = false, length = 30)
  private RebuySignal rebuySignal;

  @Builder.Default
  @Column(name = "EXEC_YN", nullable = false, length = 1)
  private String executeYn = "N";
}
