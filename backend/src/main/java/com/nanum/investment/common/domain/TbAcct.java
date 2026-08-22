package com.nanum.investment.common.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(
    name = "\"TB_ACCT\"",
    uniqueConstraints = @UniqueConstraint(name = "UK_TB_ACCT_01", columnNames = "ACCT_TP"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbAcct {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ACCT_ID")
  private Long accountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "ACCT_TP", nullable = false, length = 20)
  private AccountType accountType;

  @Builder.Default
  @Column(name = "CASH_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal cashAmount = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "RSV_CASH_AMT", nullable = false, precision = 20, scale = 4)
  private BigDecimal reservedCashAmount = BigDecimal.ZERO;

  @Column(name = "TGT_CASH_WGT", precision = 7, scale = 4)
  private BigDecimal targetCashWeight;

  @Builder.Default
  @Column(name = "DISP_SEQ", nullable = false)
  private Integer displaySequence = 0;

  @Builder.Default
  @Column(name = "DEL_YN", nullable = false, length = 1)
  private String deleteYn = "N";

  @Column(name = "CRT_DTTM", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdDateTime;

  @Column(name = "CRT_USR_ID", length = 50)
  private String createdUserId;

  @Column(name = "UPD_DTTM", nullable = false, insertable = false)
  private OffsetDateTime updatedDateTime;

  @Column(name = "UPD_USR_ID", length = 50)
  private String updatedUserId;
}
