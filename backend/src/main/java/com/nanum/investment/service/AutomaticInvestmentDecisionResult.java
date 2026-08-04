package com.nanum.investment.service;

import com.nanum.investment.domain.PortfolioDecision;
import java.time.LocalDate;
import java.util.List;

public record AutomaticInvestmentDecisionResult(
        LocalDate baseDate,
        List<String> sourceSnapshotCodes,
        int positionCount,
        long existingReservedCash,
        PortfolioDecision decision) {}
