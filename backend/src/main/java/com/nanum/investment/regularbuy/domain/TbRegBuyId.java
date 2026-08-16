package com.nanum.investment.regularbuy.domain;

import com.nanum.investment.common.domain.AccountType;
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
