package com.nanum.investment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AutomaticRebalanceResult(LocalDate baseDate,String type,List<AccountPlan> accounts,List<Item> items){
 public record AccountPlan(Long rebalanceId,Long accountId,String accountType,BigDecimal totalAssets,BigDecimal buyTotal,BigDecimal sellTotal,int itemCount,boolean required){}
 public record Item(Long rebalanceItemId,Long rebalanceId,String accountType,String stockCode,String stockName,BigDecimal currentAmount,BigDecimal targetAmount,BigDecimal currentWeight,BigDecimal targetWeight,BigDecimal currentRegularBuyAmount,BigDecimal newRegularBuyAmount,BigDecimal changeAmount,String action,Integer priority,String reason){}
}
