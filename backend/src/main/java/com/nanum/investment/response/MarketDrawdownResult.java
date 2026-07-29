package com.nanum.investment.response;

import com.nanum.investment.domain.MarketPhase;
import java.math.BigDecimal;

public record MarketDrawdownResult(
        BigDecimal currentIndex,
        BigDecimal recentPeakIndex,
        BigDecimal drawdownRate,
        MarketPhase marketPhase
) {
}
