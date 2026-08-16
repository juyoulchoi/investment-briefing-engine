package com.nanum.investment.briefing.application;

import com.nanum.investment.briefing.domain.PortfolioDecision;
import java.time.LocalDate;
import java.util.List;

public record AutomaticInvestmentDecisionResult(
    LocalDate baseDate,
    List<String> sourceSnapshotCodes,
    int positionCount,
    long existingReservedCash,
    PortfolioDecision decision) {}
