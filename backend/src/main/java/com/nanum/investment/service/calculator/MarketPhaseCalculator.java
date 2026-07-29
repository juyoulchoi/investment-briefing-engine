package com.nanum.investment.service.calculator;

import com.nanum.investment.domain.MarketPhase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MarketPhaseCalculator {
    public MarketPhase calculate(BigDecimal marketReturnRate) {
        if (marketReturnRate == null) return MarketPhase.NORMAL;
        if (marketReturnRate.compareTo(new BigDecimal("-30")) <= 0) return MarketPhase.CRASH;
        if (marketReturnRate.compareTo(new BigDecimal("-20")) <= 0) return MarketPhase.STRONG_CORRECTION;
        if (marketReturnRate.compareTo(new BigDecimal("-10")) <= 0) return MarketPhase.CORRECTION;
        return MarketPhase.NORMAL;
    }
}
