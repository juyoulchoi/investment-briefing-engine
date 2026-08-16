package com.nanum.investment.rebalance.domain;

public enum RebalanceAction {
  BUY,
  SELL,
  HOLD,
  PAUSE_REGULAR_BUY,
  REDUCE_REGULAR_BUY,
  INCREASE_REGULAR_BUY,
  RESUME_REGULAR_BUY,
  EXCLUDE
}
