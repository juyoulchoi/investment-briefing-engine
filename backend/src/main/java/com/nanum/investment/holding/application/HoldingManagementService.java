package com.nanum.investment.holding.application;

import com.nanum.investment.common.domain.AccountType;
import com.nanum.investment.common.exception.BusinessException;
import com.nanum.investment.common.exception.ErrorCode;
import com.nanum.investment.holding.api.request.HoldingBatchUpdateRequest;
import com.nanum.investment.holding.api.request.HoldingUpdateRequest;
import com.nanum.investment.holding.domain.TbHold;
import com.nanum.investment.holding.infrastructure.repository.TbHoldRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoldingManagementService {
  private final TbHoldRepository holdings;
  private final HoldingValuationService valuations;
  private final PortfolioWeightRefreshService portfolioWeights;

  public HoldingManagementService(
      TbHoldRepository holdings,
      HoldingValuationService valuations,
      PortfolioWeightRefreshService portfolioWeights) {
    this.holdings = holdings;
    this.valuations = valuations;
    this.portfolioWeights = portfolioWeights;
  }

  @Transactional
  public TbHold update(Long holdingId, HoldingUpdateRequest request) {
    TbHold holding = find(holdingId);
    apply(holding, request);
    portfolioWeights.refreshAccount(holding.getAccount());
    return holding;
  }

  @Transactional
  public List<TbHold> updateAccount(Long accountId, HoldingBatchUpdateRequest request) {
    List<TbHold> updated =
        request.updates().stream()
            .map(
                item -> {
                  TbHold holding = find(item.holdingId());
                  if (!accountId.equals(holding.getAccount().getAccountId()))
                    throw new BusinessException(
                        ErrorCode.INVALID_REQUEST, "다른 계좌의 보유종목은 함께 수정할 수 없습니다.");
                  apply(holding, item.values());
                  return holding;
                })
            .toList();
    portfolioWeights.refreshAccount(updated.getFirst().getAccount());
    return updated;
  }

  private TbHold find(Long id) {
    return holdings
        .findById(id)
        .filter(h -> "N".equals(h.getDeleteYn()))
        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "보유종목을 찾을 수 없습니다."));
  }

  private void apply(TbHold holding, HoldingUpdateRequest request) {
    holding.setHoldingQuantity(request.holdingQuantity());
    BigDecimal averagePrice = averagePrice(holding, request);
    holding.setAveragePrice(averagePrice);
    BigDecimal currentPrice =
        holding.getCurrentPrice() == null ? BigDecimal.ZERO : holding.getCurrentPrice();
    HoldingValuationService.Valuation value =
        valuations.calculate(
            request.holdingQuantity(), averagePrice, currentPrice, holding.getExchangeRate());
    holding.setOriginalEvaluationAmount(value.originalEvaluationAmount());
    holding.setEvaluationAmount(value.evaluationAmount());
    holding.setOriginalProfitLossAmount(value.originalProfitLossAmount());
    holding.setProfitLossAmount(value.profitLossAmount());
    holding.setProfitLossRate(value.profitLossRate());
    holding.setCalculatedDateTime(OffsetDateTime.now());
  }

  private BigDecimal averagePrice(TbHold holding, HoldingUpdateRequest request) {
    boolean domestic = holding.getAccount().getAccountType() == AccountType.DOMESTIC;
    BigDecimal wholeAmount = request.wholeSharePurchaseAmount();
    BigDecimal fractionalAmount = request.fractionalSharePurchaseAmount();

    if (!domestic) {
      if (wholeAmount != null || fractionalAmount != null)
        throw new BusinessException(
            ErrorCode.INVALID_REQUEST, "정수주·소수점주 매입금액은 국내주식 계좌에서만 입력할 수 있습니다.");
      holding.setWholeSharePurchaseAmount(null);
      holding.setFractionalSharePurchaseAmount(null);
      return request.averagePrice();
    }

    if (wholeAmount == null || fractionalAmount == null)
      throw new BusinessException(
          ErrorCode.INVALID_REQUEST, "국내주식 계좌는 정수주·소수점주 매입금액을 모두 입력해야 합니다.");

    BigDecimal totalPurchaseAmount = wholeAmount.add(fractionalAmount);
    if (request.holdingQuantity().signum() == 0) {
      if (totalPurchaseAmount.signum() != 0)
        throw new BusinessException(
            ErrorCode.INVALID_REQUEST, "보유수량이 0이면 정수주·소수점주 매입금액도 0이어야 합니다.");
      holding.setWholeSharePurchaseAmount(wholeAmount);
      holding.setFractionalSharePurchaseAmount(fractionalAmount);
      return BigDecimal.ZERO.setScale(6);
    }

    holding.setWholeSharePurchaseAmount(wholeAmount);
    holding.setFractionalSharePurchaseAmount(fractionalAmount);
    return totalPurchaseAmount.divide(request.holdingQuantity(), 6, RoundingMode.HALF_UP);
  }
}
