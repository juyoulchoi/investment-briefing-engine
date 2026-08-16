package com.nanum.investment.calculation;

import com.nanum.investment.domain.MarketRegime;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MarketRegimeClassifier {
  public CalculationResult<MarketRegime> classify(MarketRegimeInput i, MarketRuleSet rules) {
    if (i == null || i.marketScore() == null) throw new IllegalArgumentException("시장점수는 필수입니다.");
    if (i.emergencyEvent())
      return result(MarketRegime.CRASH_RISK, "EMERGENCY_EVENT", "비상 이벤트 강제 위험");
    if (nvl(i.structuralDamageRate()).compareTo(rules.structuralDamageCrashThreshold()) >= 0)
      return result(MarketRegime.CRASH_RISK, "STRUCTURAL_DAMAGE", "구조적 훼손 임계값 초과");
    if (nvl(i.mainIndexChangeRate()).compareTo(new BigDecimal("-5")) <= 0
        || nvl(i.volatilityIndexChangeRate()).compareTo(new BigDecimal("30")) >= 0)
      return result(MarketRegime.CRASH_RISK, "FORCED_CRASH", "급락 또는 변동성 급등 강제조건");
    BigDecimal s = i.marketScore();
    MarketRegime regime =
        s.compareTo(new BigDecimal("80")) >= 0
            ? MarketRegime.OVERHEATED
            : s.compareTo(new BigDecimal("60")) >= 0
                ? MarketRegime.NORMAL
                : s.compareTo(new BigDecimal("45")) >= 0
                    ? MarketRegime.MILD_CORRECTION
                    : s.compareTo(new BigDecimal("25")) >= 0
                        ? MarketRegime.STRONG_CORRECTION
                        : MarketRegime.CRASH_RISK;
    return result(regime, "SCORE_BAND", "시장점수 구간 " + s);
  }

  private CalculationResult<MarketRegime> result(MarketRegime v, String c, String m) {
    return new CalculationResult<>(v, List.of(new CalculationReason(c, m)));
  }

  private BigDecimal nvl(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
