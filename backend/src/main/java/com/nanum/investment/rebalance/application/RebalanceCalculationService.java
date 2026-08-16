package com.nanum.investment.rebalance.application;

import com.nanum.investment.rebalance.domain.RebalanceAction;
import com.nanum.investment.rebalance.domain.RebalanceWeightStatus;
import java.math.*;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class RebalanceCalculationService {
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  public AccountAmounts account(
      BigDecimal cash,
      BigDecimal reservedCash,
      BigDecimal holdings,
      BigDecimal targetCashWeight,
      BigDecimal newCash) {
    requireNonNegative(cash, "일반현금");
    requireNonNegative(reservedCash, "대기현금");
    requireNonNegative(holdings, "보유평가금액");
    requireNonNegative(newCash, "신규현금");
    requireWeight(targetCashWeight, "목표 현금비중");
    BigDecimal total = cash.add(reservedCash).add(holdings).add(newCash);
    BigDecimal currentCash = cash.add(reservedCash).add(newCash);
    BigDecimal currentWeight =
        total.signum() == 0
            ? BigDecimal.ZERO
            : currentCash.multiply(HUNDRED).divide(total, 4, RoundingMode.HALF_UP);
    BigDecimal targetCash =
        total.multiply(targetCashWeight).divide(HUNDRED, 4, RoundingMode.HALF_UP);
    BigDecimal gap = currentCash.subtract(targetCash);
    return new AccountAmounts(
        total, currentCash, currentWeight, targetCash, gap, gap.max(BigDecimal.ZERO));
  }

  public ItemAmounts item(
      BigDecimal totalAssets,
      BigDecimal currentAmount,
      BigDecimal targetWeight,
      BigDecimal buyBudgetRemaining,
      BigDecimal stockBuyLimit,
      BigDecimal sellTargetRemaining,
      boolean fundamentalDamaged,
      boolean tradeLimited) {
    Stream.of(totalAssets, currentAmount, buyBudgetRemaining, stockBuyLimit, sellTargetRemaining)
        .forEach(v -> requireNonNegative(v, "리밸런싱 금액"));
    requireWeight(targetWeight, "목표비중");
    BigDecimal target = totalAssets.multiply(targetWeight).divide(HUNDRED, 4, RoundingMode.HALF_UP);
    BigDecimal gap = target.subtract(currentAmount);
    BigDecimal buyNeed = gap.max(BigDecimal.ZERO);
    BigDecimal sellNeed = gap.negate().max(BigDecimal.ZERO);
    BigDecimal buy =
        fundamentalDamaged || tradeLimited
            ? BigDecimal.ZERO
            : min(buyNeed, buyBudgetRemaining, stockBuyLimit);
    BigDecimal sell =
        tradeLimited ? BigDecimal.ZERO : min(sellNeed, currentAmount, sellTargetRemaining);
    RebalanceAction action =
        buy.signum() > 0
            ? RebalanceAction.BUY
            : sell.signum() > 0 ? RebalanceAction.SELL : RebalanceAction.HOLD;
    RebalanceWeightStatus status =
        gap.signum() > 0
            ? RebalanceWeightStatus.UNDERWEIGHT
            : gap.signum() < 0 ? RebalanceWeightStatus.OVERWEIGHT : RebalanceWeightStatus.NORMAL;
    return new ItemAmounts(target, gap, buyNeed, sellNeed, buy, sell, status, action);
  }

  private BigDecimal min(BigDecimal... values) {
    return Stream.of(values).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
  }

  private void requireNonNegative(BigDecimal value, String name) {
    if (value == null || value.signum() < 0)
      throw new IllegalArgumentException(name + "은 0 이상이어야 합니다.");
  }

  private void requireWeight(BigDecimal value, String name) {
    if (value == null || value.signum() < 0 || value.compareTo(HUNDRED) > 0)
      throw new IllegalArgumentException(name + "은 0~100이어야 합니다.");
  }

  public record AccountAmounts(
      BigDecimal totalAssetAmount,
      BigDecimal currentCashAmount,
      BigDecimal currentCashWeight,
      BigDecimal targetCashAmount,
      BigDecimal cashGapAmount,
      BigDecimal buyBudgetAmount) {}

  public record ItemAmounts(
      BigDecimal targetAmount,
      BigDecimal weightGapAmount,
      BigDecimal buyNeedAmount,
      BigDecimal sellNeedAmount,
      BigDecimal recommendedBuyAmount,
      BigDecimal recommendedSellAmount,
      RebalanceWeightStatus weightStatus,
      RebalanceAction action) {}
}
