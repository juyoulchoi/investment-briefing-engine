package com.nanum.investment.briefing.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarketDirectionV2FallbackServiceTest {
  @Test
  void requiresAtLeast252HistoricalObservations() {
    assertFalse(MarketDirectionV2FallbackService.hasSufficientHistory(251));
    assertTrue(MarketDirectionV2FallbackService.hasSufficientHistory(252));
  }
}
