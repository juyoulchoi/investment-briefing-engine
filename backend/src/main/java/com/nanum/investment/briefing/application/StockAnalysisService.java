package com.nanum.investment.briefing.application;

import com.nanum.investment.briefing.api.request.StockAnalysisInput;
import com.nanum.investment.briefing.api.response.StockAnalysisResult;

public interface StockAnalysisService {
  StockAnalysisResult analyze(StockAnalysisInput input);
}
