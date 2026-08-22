package com.nanum.investment.holding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nanum.investment.common.domain.TbAcct;
import com.nanum.investment.holding.domain.TbHold;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.holding.infrastructure.repository.TbHoldRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioWeightRefreshServiceTest {
  @Mock private TbHoldRepository holdings;

  @Test
  void refreshesEveryActiveHoldingInTheAccountAndClearsExcludedHolding() {
    TbAcct account =
        TbAcct.builder().accountId(1L).cashAmount(bd("100")).reservedCashAmount(bd("100")).build();
    TbHold under = holding(account, "400", "70", "Y");
    TbHold over = holding(account, "400", "20", "Y");
    TbHold excluded = holding(account, "200", "10", "N");
    excluded.setCurrentWeight(bd("99"));
    excluded.setWeightDifferenceRate(bd("89"));
    excluded.setWeightStatus(WeightStatus.OVERWEIGHT);
    when(holdings.findAllByAccount_AccountIdAndDeleteYn(1L, "N"))
        .thenReturn(List.of(under, over, excluded));

    new PortfolioWeightRefreshService(holdings, new PortfolioWeightService())
        .refreshAccount(account);

    assertThat(under.getCurrentWeight()).isEqualByComparingTo("50.0000");
    assertThat(under.getWeightDifferenceRate()).isEqualByComparingTo("-20.0000");
    assertThat(under.getWeightStatus()).isEqualTo(WeightStatus.UNDERWEIGHT);
    assertThat(over.getCurrentWeight()).isEqualByComparingTo("50.0000");
    assertThat(over.getWeightDifferenceRate()).isEqualByComparingTo("30.0000");
    assertThat(over.getWeightStatus()).isEqualTo(WeightStatus.OVERWEIGHT);
    assertThat(under.getCalculatedDateTime()).isEqualTo(over.getCalculatedDateTime());
    assertThat(excluded.getCurrentWeight()).isNull();
    assertThat(excluded.getWeightDifferenceRate()).isNull();
    assertThat(excluded.getWeightStatus()).isNull();
  }

  @Test
  void storesCurrentWeightButNoStatusWhenTargetWeightIsMissing() {
    TbAcct account =
        TbAcct.builder()
            .accountId(2L)
            .cashAmount(BigDecimal.ZERO)
            .reservedCashAmount(BigDecimal.ZERO)
            .build();
    TbHold holding = holding(account, "250", null, "Y");
    when(holdings.findAllByAccount_AccountIdAndDeleteYn(2L, "N")).thenReturn(List.of(holding));

    new PortfolioWeightRefreshService(holdings, new PortfolioWeightService())
        .refreshAccount(account);

    assertThat(holding.getCurrentWeight()).isEqualByComparingTo("100.0000");
    assertThat(holding.getWeightDifferenceRate()).isNull();
    assertThat(holding.getWeightStatus()).isNull();
  }

  private TbHold holding(
      TbAcct account, String evaluationAmount, String targetWeight, String useYn) {
    return TbHold.builder()
        .account(account)
        .evaluationAmount(bd(evaluationAmount))
        .targetWeight(targetWeight == null ? null : bd(targetWeight))
        .deleteYn("N")
        .build();
  }

  private BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
