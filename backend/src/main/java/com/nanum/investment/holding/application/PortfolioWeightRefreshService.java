package com.nanum.investment.holding.application;

import com.nanum.investment.common.domain.TbAcct;
import com.nanum.investment.holding.domain.TbHold;
import com.nanum.investment.holding.infrastructure.repository.TbHoldRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class PortfolioWeightRefreshService {
  private final TbHoldRepository holdings;
  private final PortfolioWeightService weights;
  private final JdbcClient jdbc;

  public PortfolioWeightRefreshService(
      TbHoldRepository holdings, PortfolioWeightService weights, JdbcClient jdbc) {
    this.holdings = holdings;
    this.weights = weights;
    this.jdbc = jdbc;
  }

  public void refreshAccount(TbAcct account) {
    List<TbHold> accountHoldings =
        holdings.findAllByAccount_AccountIdAndDeleteYn(account.getAccountId(), "N");
    BigDecimal exchangeRate =
        com.nanum.investment.common.domain.AccountType.OVERSEAS.equals(account.getAccountType())
            ? usdKrw()
            : BigDecimal.ONE;
    BigDecimal totalHoldings =
        accountHoldings.stream()
            .filter(PortfolioWeightRefreshService::isActive)
            .map(holding -> normalizedEvaluation(holding, exchangeRate))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalAccountAssets = totalHoldings.add(nvl(account.getCashAmount()));
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
              normalizedEvaluation(holding, exchangeRate),
              totalAccountAssets,
              holding.getTargetWeight());
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

  private BigDecimal usdKrw() {
    return jdbc.sql(
            "SELECT COALESCE((SELECT \"EXCH_RT\" FROM \"TB_EXCH_DAY\" WHERE \"BASE_CURR_CD\"='USD' AND \"QUOTE_CURR_CD\"='KRW' ORDER BY \"BASE_DT\" DESC LIMIT 1),1)")
        .query(BigDecimal.class)
        .single();
  }

  private static BigDecimal normalizedEvaluation(TbHold holding, BigDecimal exchangeRate) {
    if (exchangeRate.compareTo(BigDecimal.ONE) != 0 && holding.getOriginalEvaluationAmount() != null)
      return holding.getOriginalEvaluationAmount().multiply(exchangeRate);
    return nvl(holding.getEvaluationAmount());
  }
}
