package com.nanum.investment.rebalance.domain;

public enum RebalanceStatus {
  DRAFT,
  CALCULATED,
  REVIEWED,
  APPROVED,
  PARTIALLY_EXECUTED,
  COMPLETED,
  CANCELLED
}
