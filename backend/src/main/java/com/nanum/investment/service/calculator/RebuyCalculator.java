package com.nanum.investment.service.calculator;

import com.nanum.investment.domain.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class RebuyCalculator {
    public RebuySignal calculate(BigDecimal drawdownRate, boolean aboveMa5, boolean aboveMa20,
            WeightStatus weightStatus, RiskLevel riskLevel, MarketPhase marketPhase,
            boolean benchmarkStable, boolean existingRiskResolved, boolean rebuyEligible) {
        if (!rebuyEligible || weightStatus != WeightStatus.UNDER
                || riskLevel == RiskLevel.VERY_HIGH
                || marketPhase == MarketPhase.STRONG_CORRECTION || marketPhase == MarketPhase.CRASH)
            return RebuySignal.WAIT;
        if (aboveMa20 && benchmarkStable && existingRiskResolved
                && (marketPhase == MarketPhase.NORMAL || marketPhase == MarketPhase.CORRECTION)
                && riskLevel.ordinal() <= RiskLevel.MEDIUM.ordinal()) return RebuySignal.ALLOWED;
        if (drawdownRate != null && drawdownRate.compareTo(new BigDecimal("-15")) <= 0
                && aboveMa5 && benchmarkStable && existingRiskResolved) return RebuySignal.PARTIAL;
        if (drawdownRate != null && drawdownRate.compareTo(new BigDecimal("-10")) <= 0)
            return RebuySignal.WATCH;
        return RebuySignal.WAIT;
    }
}
