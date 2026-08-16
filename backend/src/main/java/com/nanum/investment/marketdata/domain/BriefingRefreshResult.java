package com.nanum.investment.marketdata.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record BriefingRefreshResult(
    boolean success,
    LocalDate baseDate,
    LocalDate krxBaseDate,
    Map<String, Integer> krxReceivedCounts,
    int overseasRequestedCount,
    int overseasSuccessCount,
    List<String> overseasSuccessSymbols,
    List<String> completedSteps,
    Map<String, Object> stepResults,
    List<String> failures) {}
