package com.nanum.investment.briefing.api.response;

import com.nanum.investment.marketdata.domain.MarketPhase;
import java.math.BigDecimal;

public record MarketDrawdownResult(
    BigDecimal currentIndex,
    BigDecimal recentPeakIndex,
    BigDecimal drawdownRate,
    MarketPhase marketPhase) {}
