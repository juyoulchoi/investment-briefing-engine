package com.nanum.investment.service;

import com.nanum.investment.response.InvestmentBriefingResponse;

public interface BriefingAiClient {
    InvestmentBriefingResponse generateBriefing();
}
