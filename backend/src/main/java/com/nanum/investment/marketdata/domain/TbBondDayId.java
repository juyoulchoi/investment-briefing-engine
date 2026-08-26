package com.nanum.investment.marketdata.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class TbBondDayId implements Serializable {
  @Column(name = "BOND_CD", nullable = false, length = 30)
  private String bondCode;

  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;
}
