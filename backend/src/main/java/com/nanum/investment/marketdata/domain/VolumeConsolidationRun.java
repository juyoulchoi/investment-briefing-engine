package com.nanum.investment.marketdata.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VolumeConsolidationRun(
    UUID runId,
    String triggerCode,
    LocalDate collectionBaseDate,
    LocalDate baseDate,
    String ruleVersion,
    String status,
    Integer matchCount,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    String failureReason,
    boolean hasResult) {}
