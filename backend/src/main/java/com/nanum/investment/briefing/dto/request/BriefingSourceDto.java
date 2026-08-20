package com.nanum.investment.briefing.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

public record BriefingSourceDto(
    LocalDate briefingDate,
    String briefingType,
    MarketRiskDto marketRisk,
    String marketPhase,
    MarketDirectionDto marketDirection,
    String regularBuySignal,
    String dailyActionSignal,
    int recommendedCashRatio,
    List<AccountActionDto> accounts,
    List<SectorSignalDto> sectors,
    List<HoldingSignalDto> holdings,
    JsonNode dbSnapshot) {}
