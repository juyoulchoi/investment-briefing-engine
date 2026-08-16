package com.nanum.investment.service;

import com.nanum.investment.request.StockAnalysisInput;
import com.nanum.investment.response.StockAnalysisResult;

public interface StockAnalysisService {
  StockAnalysisResult analyze(StockAnalysisInput input);
}
