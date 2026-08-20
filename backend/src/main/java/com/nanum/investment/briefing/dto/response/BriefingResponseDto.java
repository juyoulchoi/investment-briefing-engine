package com.nanum.investment.briefing.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.briefing.api.response.BriefingItemResponse;
import java.time.LocalDate;
import java.util.List;

public record BriefingResponseDto(
    LocalDate briefingDate,
    String briefingType,
    String title,
    JsonNode confirmedValues,
    List<BriefingItemResponse> items) {}
