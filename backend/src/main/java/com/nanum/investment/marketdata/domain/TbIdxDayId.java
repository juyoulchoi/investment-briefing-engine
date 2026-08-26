package com.nanum.investment.marketdata.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class TbIdxDayId implements Serializable {
  @Column(name = "IDX_CD", nullable = false, length = 30)
  private String indexCode;

  @Column(name = "TRADE_DT", nullable = false)
  private LocalDate tradeDate;
}
