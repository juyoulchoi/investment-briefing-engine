package com.nanum.investment.briefing.application;

import com.nanum.investment.marketdata.domain.MarketSnapshot;
import com.nanum.investment.marketdata.domain.SentimentAssessment;
import com.nanum.investment.marketdata.domain.SentimentPhase;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class MarketSentimentService {
  public SentimentAssessment assess(MarketSnapshot s) {
    int risk = 0;
    List<String> reasons = new ArrayList<>();
    List<String> reversal = new ArrayList<>();
    risk += w(s.newsFearScore(), .25);
    risk += w(s.aiCapexFatigueScore(), .25);
    risk += w(100 - s.earningsConfidenceScore(), .20);
    risk += w(100 - s.liquidityScore(), .15);
    if (s.volatilityIndex() >= 35) {
      risk += 12;
      reasons.add("변동성 지수가 패닉 구간입니다.");
    } else if (s.volatilityIndex() >= 25) {
      risk += 7;
      reasons.add("변동성 지수가 경계 구간입니다.");
    }
    if (s.foreignNetBuy() < 0 && s.programNetBuy() < 0) {
      risk += 8;
      reasons.add("외국인과 프로그램 매도가 동시에 발생했습니다.");
    }
    if (s.usdKrwChangePct() >= 1) {
      risk += 5;
      reasons.add("원화 약세가 외국인 위험 축소 압력을 높이고 있습니다.");
    }
    if (s.us10yChangeBp() >= 10) {
      risk += 5;
      reasons.add("미국 장기금리 상승으로 성장주의 할인율 부담이 커졌습니다.");
    }
    if (s.aiCapexFatigueScore() >= 70 && s.earningsConfidenceScore() >= 65)
      reasons.add("실적은 유지되지만 AI 투자 회수 시점에 대한 피로감이 큽니다.");
    boolean structural =
        s.earningsConfidenceScore() < 45 && s.liquidityScore() < 45 && s.indexDrawdownPct() <= -20;
    if (structural) reasons.add("실적·유동성·가격이 함께 훼손되어 구조적 하락 위험이 있습니다.");
    reversal.addAll(
        List.of(
            "AI 서비스 매출 증가율 개선",
            "CAPEX 증가율 둔화 또는 투자 회수 기간 단축",
            "외국인 현물·선물 동반 순매수 전환",
            "장기금리와 원·달러 환율 안정",
            "자유현금흐름 개선"));
    risk = Math.max(0, Math.min(100, risk));
    int confidence =
        Math.min(
            95,
            55
                + Math.min(20, Math.abs(s.newsFearScore() - 50) / 2)
                + Math.min(15, Math.abs(s.aiCapexFatigueScore() - 50) / 3));
    return new SentimentAssessment(
        phase(risk), risk, confidence, structural, List.copyOf(reasons), List.copyOf(reversal));
  }

  private int w(int v, double x) {
    return (int) Math.round(v * x);
  }

  private SentimentPhase phase(int r) {
    if (r >= 85) return SentimentPhase.PANIC;
    if (r >= 70) return SentimentPhase.FEAR;
    if (r >= 55) return SentimentPhase.FATIGUE;
    if (r >= 40) return SentimentPhase.NEUTRAL;
    if (r >= 20) return SentimentPhase.OPTIMISM;
    return SentimentPhase.GREED;
  }
}
