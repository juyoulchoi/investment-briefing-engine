package com.nanum.investment.briefing.api.response;

public record BriefingItemResponse(
    String itemCode, String summary, String content, String signalCode, boolean actionRequired) {}
