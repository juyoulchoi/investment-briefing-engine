package com.nanum.investment.briefing.api;

import com.nanum.investment.holding.domain.StockPosition;
import com.nanum.investment.marketdata.domain.MarketSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

public record InvestmentDecisionRequest(
    LocalDate decisionDate,
    @Valid MarketSnapshot market,
    @NotEmpty List<@Valid StockPosition> positions,
    long existingReservedCash) {}
