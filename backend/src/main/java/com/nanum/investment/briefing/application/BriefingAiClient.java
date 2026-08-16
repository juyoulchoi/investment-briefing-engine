package com.nanum.investment.briefing.application;

import com.nanum.investment.briefing.api.response.InvestmentBriefingResponse;
import com.nanum.investment.briefing.domain.BriefingType;

public interface BriefingAiClient {
  InvestmentBriefingResponse generateBriefing(BriefingType briefingType);

  default InvestmentBriefingResponse generateBriefing() {
    return generateBriefing(BriefingType.DAILY);
  }
}
