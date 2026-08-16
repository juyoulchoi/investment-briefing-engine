package com.nanum.investment.briefing.application;

import com.nanum.investment.marketdata.domain.DataStatus;
import java.time.LocalDate;
import java.util.List;

public record BriefingRawDataResult(
    Long briefingId,
    LocalDate baseDate,
    Integer calculationSequence,
    Long investmentDecisionId,
    DataStatus dataStatus,
    int confidence,
    String sha256,
    List<String> sections) {}
