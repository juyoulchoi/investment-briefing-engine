package com.nanum.investment.service;

import com.nanum.investment.domain.BriefingType;
import com.nanum.investment.response.InvestmentBriefingResponse;

public interface BriefingAiClient {
    InvestmentBriefingResponse generateBriefing(BriefingType briefingType);

    default InvestmentBriefingResponse generateBriefing() {
        return generateBriefing(BriefingType.DAILY);
    }
}
