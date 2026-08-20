package com.nanum.investment.briefing.application;

import java.util.List;

public final class BriefingItemCatalog {
  private BriefingItemCatalog() {}
  public static final List<Item> ITEMS = List.of(
      new Item("MARKET_RISK", "시장 위험지수"),
      new Item("MARKET_PHASE", "시장 국면"),
      new Item("US_MARKET", "전일 미국시장"),
      new Item("KR_MARKET_PREVIOUS", "전날 한국 주식시장 상황"),
      new Item("KR_MARKET_OUTLOOK", "한국시장 예상"),
      new Item("MARKET_DIRECTION", "1~4주 시장 방향 예측"),
      new Item("SECTOR_SIGNALS", "업종별 신호등"),
      new Item("REGULAR_BUY", "정기매수 판단"),
      new Item("ACCOUNT_ACTIONS", "계좌별 행동"),
      new Item("HOLDING_SIGNALS", "보유 종목별 신호등"),
      new Item("ADDITIONAL_BUYS", "추가매수 후보"),
      new Item("REBUY_SIGNALS", "재매수 신호"),
      new Item("ACTION_SIGNAL", "당일 행동신호"),
      new Item("SCHEDULE_AND_RISKS", "주요 일정과 위험요인"),
      new Item("CONCLUSION", "오늘의 결론"));
  public static String titleOf(String code) {
    return ITEMS.stream().filter(item -> item.code().equals(code)).map(Item::title).findFirst().orElse(code);
  }
  public record Item(String code, String title) {}
}
