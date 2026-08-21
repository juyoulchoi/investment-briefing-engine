package com.nanum.investment.holding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanum.investment.common.domain.AccountType;
import com.nanum.investment.common.domain.TbAcct;
import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.holding.api.request.HoldingUpdateRequest;
import com.nanum.investment.holding.domain.TbHold;
import com.nanum.investment.holding.infrastructure.repository.TbHoldRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HoldingManagementServiceTest {
  @Mock private TbHoldRepository holdings;
  @Mock private PortfolioWeightRefreshService portfolioWeights;

  @Test
  void recalculatesDomesticAveragePriceFromWholeAndFractionalPurchaseAmounts() {
    TbHold holding = holding(AccountType.DOMESTIC);
    when(holdings.findById(1L)).thenReturn(Optional.of(holding));
    HoldingManagementService service = service();

    service.update(1L, new HoldingUpdateRequest(bd("1.1"), bd("999999"), bd("105000"), bd("5000")));

    assertThat(holding.getAveragePrice()).isEqualByComparingTo("100000.000000");
    assertThat(holding.getWholeSharePurchaseAmount()).isEqualByComparingTo("105000");
    assertThat(holding.getFractionalSharePurchaseAmount()).isEqualByComparingTo("5000");
    verify(portfolioWeights).refreshAccount(holding.getAccount());
  }

  @Test
  void rejectsDomesticUpdateWithoutBothPurchaseAmounts() {
    TbHold holding = holding(AccountType.DOMESTIC);
    when(holdings.findById(1L)).thenReturn(Optional.of(holding));

    assertThatThrownBy(
            () ->
                service()
                    .update(
                        1L, new HoldingUpdateRequest(bd("1"), bd("100000"), bd("100000"), null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("모두 입력");
  }

  @Test
  void rejectsPurchaseAmountsForNonDomesticAccount() {
    TbHold holding = holding(AccountType.ISA);
    when(holdings.findById(1L)).thenReturn(Optional.of(holding));

    assertThatThrownBy(
            () ->
                service()
                    .update(
                        1L, new HoldingUpdateRequest(bd("1"), bd("100000"), bd("100000"), bd("0"))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("국내주식 계좌에서만");
  }

  private HoldingManagementService service() {
    return new HoldingManagementService(holdings, new HoldingValuationService(), portfolioWeights);
  }

  private TbHold holding(AccountType type) {
    return TbHold.builder()
        .holdingId(1L)
        .account(TbAcct.builder().accountId(1L).accountType(type).build())
        .holdingQuantity(BigDecimal.ZERO)
        .averagePrice(BigDecimal.ZERO)
        .currentPrice(bd("120000"))
        .exchangeRate(BigDecimal.ONE)
        .deleteYn("N")
        .build();
  }

  private BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
