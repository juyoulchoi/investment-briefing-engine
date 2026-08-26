package com.nanum.investment.marketdata.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@IdClass(TbBondDayId.class)
@Table(
    name = "\"TB_FRED_BOND_DAY\"",
    uniqueConstraints =
        @UniqueConstraint(
            name = "PK_TB_FRED_BOND_DAY",
            columnNames = {"BASE_DT", "BOND_CD"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbBondDay {
  @Id
  @Column(name = "BOND_CD", nullable = false, length = 30)
  private String bondCode;

  @Id
  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;

  @Column(name = "BOND_NM", nullable = false, length = 100)
  private String bondName;

  @Column(name = "CNTRY_CD", nullable = false, length = 10)
  private String countryCode;

  @Column(name = "MATURITY_MON")
  private Integer maturityMonths;

  @Column(name = "YLD_RT", nullable = false, precision = 10, scale = 6)
  private BigDecimal yieldRate;

  @Column(name = "PREV_YLD_RT", precision = 10, scale = 6)
  private BigDecimal previousYieldRate;

  @Column(name = "CHG_BP", precision = 10, scale = 4)
  private BigDecimal changeBasisPoints;

  @Column(name = "DATA_SRC_CD", nullable = false, length = 30)
  private String dataSourceCode;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "DATA_STS", nullable = false, length = 20)
  private DataStatus dataStatus = DataStatus.FRESH;
}
