package com.nanum.investment.briefing.dto.request;

public record MarketScenarioProbabilityDto(
    int uptrendResume, int boxRange, int reCorrection, int retestLow,
    int uptrendResumeChange, int boxRangeChange, int reCorrectionChange, int retestLowChange) {}
