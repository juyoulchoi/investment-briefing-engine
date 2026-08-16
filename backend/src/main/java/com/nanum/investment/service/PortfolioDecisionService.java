package com.nanum.investment.service;

import com.nanum.investment.api.*;
import com.nanum.investment.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PortfolioDecisionService {
  private final MarketRegimeService market;
  private final RegularBuyRuleService buy;

  public PortfolioDecisionService(MarketRegimeService m, RegularBuyRuleService b) {
    market = m;
    buy = b;
  }

  public PortfolioDecision decide(InvestmentDecisionRequest req) {
    LocalDate d = req.decisionDate() == null ? LocalDate.now() : req.decisionDate();
    MarketAssessment ma = market.assess(req.market());
    List<StockDecision> ds = req.positions().stream().map(p -> buy.decide(p, ma)).toList();
    long min = ds.stream().mapToLong(StockDecision::minimumBuyAmount).sum();
    long rec = ds.stream().mapToLong(StockDecision::recommendedBuyAmount).sum();
    long reserve = ds.stream().mapToLong(StockDecision::reservedCash).sum();
    return new PortfolioDecision(
        d,
        ma,
        min,
        rec,
        reserve,
        req.existingReservedCash() + reserve,
        ds,
        weekly(d, ma, ds),
        monthly(d, req.positions()));
  }

  private List<String> weekly(LocalDate d, MarketAssessment m, List<StockDecision> ds) {
    List<String> a = new ArrayList<>();
    if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
      a.add("주간 확정일이 아니므로 참고 신호로만 사용합니다.");
      return a;
    }
    a.addAll(
        List.of(
            "다음 주 정기매수 금액을 최소금액 배수로 확정합니다.",
            "목표비중과 종목 구성은 변경하지 않습니다.",
            "감액분은 추가매수 대기 현금으로 적립합니다."));
    a.add(
        "증액 대상 "
            + ds.stream().filter(x -> x.multiplier() > 1).count()
            + "개, 중단 대상 "
            + ds.stream().filter(x -> x.action() == ActionSignal.PAUSE).count()
            + "개");
    a.add("현재 시장 국면: " + m.regime());
    return a;
  }

  private List<String> monthly(LocalDate d, List<StockPosition> ps) {
    List<String> a = new ArrayList<>();
    if (d.getDayOfMonth() < 25) {
      a.add("월간 목표비중 리밸런싱 기간이 아닙니다.");
      return a;
    }
    a.addAll(List.of("목표비중·섹터비중·종목 편입 여부를 검토합니다.", "주간 변동만으로 종목을 교체하지 않습니다."));
    ps.stream()
        .filter(StockPosition::isOverweight)
        .forEach(p -> a.add(p.name() + "은 목표비중 120% 초과로 신규매수를 중단합니다."));
    return a;
  }
}
