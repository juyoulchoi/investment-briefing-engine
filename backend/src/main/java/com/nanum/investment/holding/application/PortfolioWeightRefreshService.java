package com.nanum.investment.holding.application;

import com.nanum.investment.common.domain.TbAcct;
import com.nanum.investment.holding.domain.TbHold;
import com.nanum.investment.holding.infrastructure.repository.TbHoldRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortfolioWeightRefreshService {
  private final TbHoldRepository holdings;
  private final PortfolioWeightService weights;

  public PortfolioWeightRefreshService(TbHoldRepository holdings, PortfolioWeightService weights) {
    this.holdings = holdings;
    this.weights = weights;
  }

  public void refreshAccount(TbAcct account) {
    List<TbHold> accountHoldings =
        holdings.findAllByAccount_AccountIdAndDeleteYn(account.getAccountId(), "N");
    BigDecimal totalHoldings =
        accountHoldings.stream()
            .filter(PortfolioWeightRefreshService::isActive)
            .map(TbHold::getEvaluationAmount)
            .map(PortfolioWeightRefreshService::nvl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    OffsetDateTime calculatedAt = OffsetDateTime.now();

    for (TbHold holding : accountHoldings) {
      if (!isActive(holding)) {
        holding.setCurrentWeight(null);
        holding.setWeightDifferenceRate(null);
        holding.setWeightStatus(null);
        continue;
      }

      PortfolioWeightService.Result result =
          weights.calculateHoldingsWeight(
              holding.getEvaluationAmount(), totalHoldings, holding.getTargetWeight());
      holding.setCurrentWeight(result.currentWeight());
      holding.setWeightDifferenceRate(result.weightDifferenceRate());
      holding.setWeightStatus(result.weightStatus());
      holding.setCalculatedDateTime(calculatedAt);
    }
  }

  private static boolean isActive(TbHold holding) {
    return "Y".equals(holding.getUseYn());
  }

  private static BigDecimal nvl(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
