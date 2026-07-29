package com.nanum.investment.service.impl;

import com.nanum.investment.response.InvestmentBriefingResponse;
import com.nanum.investment.service.BriefingAiClient;
import org.springframework.stereotype.Component;

@Component
public class UnsupportedBriefingAiClient implements BriefingAiClient {
    @Override
    public InvestmentBriefingResponse generateBriefing() {
        throw new IllegalStateException("BriefingAiClient 구현과 AI 공급자 설정이 필요합니다.");
    }
}
