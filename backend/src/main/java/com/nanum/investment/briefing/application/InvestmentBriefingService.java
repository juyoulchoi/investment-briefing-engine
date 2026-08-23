package com.nanum.investment.briefing.application;

import com.nanum.investment.briefing.domain.BriefingType;

public interface InvestmentBriefingService {
  Long generateAndSave(BriefingType briefingType);

  default Long generateAndSave() {
    return generateAndSave(BriefingType.DAILY);
  }
}
