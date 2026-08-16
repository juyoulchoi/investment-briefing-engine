package com.nanum.investment.domain;

import java.io.Serializable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TbRegBuyId implements Serializable {
  private AccountType accountType;
  private String stockCode;
}
