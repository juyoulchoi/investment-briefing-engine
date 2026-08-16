package com.nanum.investment.marketdata;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record HoldingMarketDataRefreshResult(
    boolean success,
    LocalDate krxBaseDate,
    Map<String, Integer> krxReceivedCounts,
    int overseasRequestedCount,
    int overseasSuccessCount,
    List<String> overseasSuccessSymbols,
    List<String> failures) {}
