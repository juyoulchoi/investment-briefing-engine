package com.nanum.investment.domain;

import java.util.List;

public record SentimentAssessment(
    SentimentPhase phase,
    int sentimentRiskScore,
    int confidence,
    boolean structuralDamageRisk,
    List<String> reasons,
    List<String> reversalSignals) {}
