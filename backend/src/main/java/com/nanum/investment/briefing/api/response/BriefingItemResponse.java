package com.nanum.investment.briefing.api.response;

import com.nanum.investment.briefing.application.BriefingItemCatalog;

public record BriefingItemResponse(
    String itemCode, String title, String summary, String content, String signalCode, boolean actionRequired) {
  public BriefingItemResponse(String itemCode, String summary, String content, String signalCode, boolean actionRequired) {
    this(itemCode, BriefingItemCatalog.titleOf(itemCode), summary, content, signalCode, actionRequired);
  }
}
