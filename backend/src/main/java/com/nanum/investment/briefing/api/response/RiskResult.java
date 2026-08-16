package com.nanum.investment.briefing.api.response;

import com.nanum.investment.common.domain.RiskLevel;
import java.util.List;

public record RiskResult(int score, RiskLevel level, List<String> reasons) {}
