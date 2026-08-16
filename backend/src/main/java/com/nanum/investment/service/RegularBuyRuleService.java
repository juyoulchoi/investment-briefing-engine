package com.nanum.investment.service;

import com.nanum.investment.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class RegularBuyRuleService {
  public StockDecision decide(StockPosition p, MarketAssessment m) {
    List<String> r = new ArrayList<>();
    if (!p.regularBuyEnabled())
      return build(p, ActionSignal.PAUSE, 0, r, "사용자가 정기매수를 일시정지한 종목입니다.");
    if (p.isOverweight()) return build(p, ActionSignal.PAUSE, 0, r, "현재 비중이 목표비중의 120%를 초과했습니다.");
    if (p.fundamentalScore() < 45 || p.themeRiskScore() >= 85)
      return build(p, ActionSignal.PAUSE, 0, r, "펀더멘털 훼손 또는 테마 위험이 높습니다.");
    if (m.sentiment().structuralDamageRisk())
      return build(p, ActionSignal.REDUCE, .5, r, "시장 구조적 훼손 가능성 때문에 절반만 집행합니다.");
    double x =
        switch (m.regime()) {
          case OVERHEATED -> .5;
          case NORMAL -> 1;
          case MILD_CORRECTION -> 1.5;
          case STRONG_CORRECTION -> 2;
          case CRASH_RISK -> .5;
        };
    r.add("시장 국면 기본 배수 " + x + "배");
    if (p.fundamentalScore() >= 75) {
      x += .5;
      r.add("펀더멘털 우수 +0.5배");
    }
    if (p.valuationScore() >= 70 && p.stockDrawdownPct() <= -15) {
      x += .5;
      r.add("밸류에이션·낙폭 조건 +0.5배");
    }
    if (m.sentiment().phase() == SentimentPhase.PANIC
        && p.fundamentalScore() >= 70
        && !m.sentiment().structuralDamageRisk()) {
      x += .5;
      r.add("건전한 펀더멘털의 패닉 구간 +0.5배");
    }
    if (p.themeRiskScore() >= 65) {
      x -= .5;
      r.add("테마 위험 -0.5배");
    }
    if (p.currentWeight() > p.targetWeight()) {
      x = Math.min(x, 1);
      r.add("목표비중 초과로 1배 제한");
    }
    x = Math.max(0, Math.min(p.effectiveMaximumIncreaseMultiplier(), x));
    ActionSignal a =
        x == 0
            ? ActionSignal.PAUSE
            : x < 1
                ? ActionSignal.REDUCE
                : x == 1
                    ? ActionSignal.KEEP_MINIMUM
                    : x < 2 ? ActionSignal.INCREASE : ActionSignal.ADDITIONAL_BUY;
    return finish(p, a, x, r);
  }

  private StockDecision build(
      StockPosition p, ActionSignal a, double x, List<String> r, String reason) {
    r.add(reason);
    return finish(p, a, x, r);
  }

  private StockDecision finish(StockPosition p, ActionSignal a, double x, List<String> r) {
    long rec = Math.min(Math.round(p.minimumBuyAmount() * x), p.effectiveMaximumBuyAmount());
    long reserve = Math.max(0, p.minimumBuyAmount() - rec);
    String adjustmentReason = String.join(" ", r);
    String cashPlan =
        reserve > 0
            ? reserve + "원을 현금으로 확보하고 다음 추가매수 신호까지 대기합니다."
            : x > 1
                ? "추가 투입분 " + Math.max(0, rec - p.minimumBuyAmount()) + "원을 사용합니다."
                : "별도 확보 현금 없이 기본 투자계획을 유지합니다.";
    return new StockDecision(
        p.account(),
        p.code(),
        p.name(),
        a,
        x,
        p.minimumBuyAmount(),
        p.effectiveMaximumBuyAmount(),
        p.effectiveMaximumIncreaseMultiplier(),
        rec,
        reserve,
        adjustmentReason,
        cashPlan,
        List.copyOf(r));
  }
}
