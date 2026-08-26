package com.nanum.investment.marketdata.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@IdClass(TbIdxDayId.class)
@Table(
    name = "\"TB_IDX_DAY\"",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_TB_IDX_DAY_01",
            columnNames = {"IDX_CD", "TRADE_DT"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbIdxDay {
  @Id
  @Column(name = "IDX_CD", nullable = false, length = 30)
  private String indexCode;

  @Id
  @Column(name = "TRADE_DT", nullable = false)
  private LocalDate tradeDate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "IDX_CD", nullable = false, insertable = false, updatable = false)
  private TbIdx index;

  @Column(name = "OPEN_VAL", precision = 20, scale = 6)
  private BigDecimal openValue;

  @Column(name = "HIGH_VAL", precision = 20, scale = 6)
  private BigDecimal highValue;

  @Column(name = "LOW_VAL", precision = 20, scale = 6)
  private BigDecimal lowValue;

  @Column(name = "CLS_VAL", precision = 20, scale = 6)
  private BigDecimal closeValue;

  @Column(name = "CHG_RT", precision = 10, scale = 4)
  private BigDecimal changeRate;

  @Column(name = "DD_HIGH_RT", precision = 10, scale = 4)
  private BigDecimal drawdownFromHighRate;

  @Column(name = "DATA_SRC_CD", nullable = false, length = 30)
  private String dataSourceCode;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "DATA_STS", nullable = false, length = 20)
  private DataStatus dataStatus = DataStatus.FRESH;
}
