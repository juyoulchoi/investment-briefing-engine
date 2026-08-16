package com.nanum.investment.response;

public record BriefingItemResponse(
    String itemCode, String summary, String content, String signalCode, boolean actionRequired) {}
