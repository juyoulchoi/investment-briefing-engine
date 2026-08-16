package com.nanum.investment.briefing.domain.calculator;

import com.nanum.investment.briefing.api.request.RiskInput;
import com.nanum.investment.briefing.api.response.RiskResult;
import com.nanum.investment.common.domain.RiskLevel;
import com.nanum.investment.holding.domain.WeightStatus;
import com.nanum.investment.marketdata.domain.MarketPhase;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RiskCalculator {
  public RiskResult calculate(RiskInput input) {
    int score = 0;
    List<String> reasons = new ArrayList<>();

    if (input.individualStock()) {
      score += 2;
      reasons.add("개별주 +2");
    }
    if (input.highRiskProduct()) {
      score += 3;
      reasons.add("레버리지·테마 ETF +3");
    }

    BigDecimal stockReturnRate =
        input.stockReturnRate() == null ? BigDecimal.ZERO : input.stockReturnRate();
    if (stockReturnRate.compareTo(new BigDecimal("-20")) <= 0) {
      score += 2;
      reasons.add("종목 수익률 -20% 이하 +2");
    }
    if (stockReturnRate.compareTo(new BigDecimal("-30")) <= 0) {
      score += 2;
      reasons.add("종목 수익률 -30% 이하 추가 +2");
    }
    if (input.weightStatus() == WeightStatus.OVER) {
      score += 2;
      reasons.add("목표비중 초과 +2");
    }
    if (input.marketPhase() == MarketPhase.STRONG_CORRECTION) {
      score += 1;
      reasons.add("시장 강한조정 +1");
    }
    if (input.marketPhase() == MarketPhase.CRASH) {
      score += 2;
      reasons.add("시장 대폭락 +2");
    }
    if (input.accumulationPaused()) {
      score += 1;
      reasons.add("정기매수 일시정지 +1");
    }

    RiskLevel level;
    if (score >= 7) level = RiskLevel.VERY_HIGH;
    else if (score >= 5) level = RiskLevel.HIGH;
    else if (score >= 3) level = RiskLevel.MEDIUM;
    else level = RiskLevel.LOW;

    if (reasons.isEmpty()) reasons.add("위험 가점 조건 없음");
    return new RiskResult(score, level, List.copyOf(reasons));
  }
}
