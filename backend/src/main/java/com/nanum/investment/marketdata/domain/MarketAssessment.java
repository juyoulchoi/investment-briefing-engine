package com.nanum.investment.marketdata.domain;

import java.util.List;

public record MarketAssessment(
    MarketRegime regime, int marketScore, SentimentAssessment sentiment, List<String> reasons) {}
