package com.nanum.investment.response;

import com.nanum.investment.domain.RiskLevel;
import java.util.List;

public record RiskResult(int score, RiskLevel level, List<String> reasons) {}
