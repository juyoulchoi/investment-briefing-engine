package com.nanum.investment.domain;
import lombok.*; import java.io.Serializable;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class TbRegBuyId implements Serializable { private AccountType accountType; private String stockCode; }

