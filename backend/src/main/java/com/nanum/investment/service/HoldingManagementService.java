package com.nanum.investment.service;

import com.nanum.investment.common.exception.*;
import com.nanum.investment.domain.TbHold;
import com.nanum.investment.repository.TbHoldRepository;
import com.nanum.investment.request.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class HoldingManagementService {
 private final TbHoldRepository holdings;
 private final HoldingValuationService valuations;
 public HoldingManagementService(TbHoldRepository holdings,HoldingValuationService valuations){this.holdings=holdings;this.valuations=valuations;}

 @Transactional
 public TbHold update(Long holdingId,HoldingUpdateRequest request){
  TbHold holding=find(holdingId);
  apply(holding,request);
  return holding;
 }

 @Transactional
 public List<TbHold> updateAccount(Long accountId,HoldingBatchUpdateRequest request){
  return request.updates().stream().map(item->{
   TbHold holding=find(item.holdingId());
   if(!accountId.equals(holding.getAccount().getAccountId()))throw new BusinessException(ErrorCode.INVALID_REQUEST,"다른 계좌의 보유종목은 함께 수정할 수 없습니다.");
   apply(holding,item.values());
   return holding;
  }).toList();
 }

 private TbHold find(Long id){return holdings.findById(id).filter(h->"N".equals(h.getDeleteYn())).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,"보유종목을 찾을 수 없습니다."));}
 private void apply(TbHold holding,HoldingUpdateRequest request){
  holding.setHoldingQuantity(request.holdingQuantity());
  holding.setAveragePrice(request.averagePrice());
  BigDecimal currentPrice=holding.getCurrentPrice()==null?BigDecimal.ZERO:holding.getCurrentPrice();
  HoldingValuationService.Valuation value=valuations.calculate(request.holdingQuantity(),request.averagePrice(),currentPrice,holding.getExchangeRate());
  holding.setOriginalEvaluationAmount(value.originalEvaluationAmount());
  holding.setEvaluationAmount(value.evaluationAmount());
  holding.setOriginalProfitLossAmount(value.originalProfitLossAmount());
  holding.setProfitLossAmount(value.profitLossAmount());
  holding.setProfitLossRate(value.profitLossRate());
  holding.setCalculatedDateTime(OffsetDateTime.now());
 }
}
