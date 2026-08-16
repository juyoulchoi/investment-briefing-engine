package com.nanum.investment.service;

import com.nanum.investment.domain.BriefingType;
import com.nanum.investment.domain.TbInvestmentBriefing;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvestmentBriefingService {
  Long generateAndSave(BriefingType briefingType);

  default Long generateAndSave() {
    return generateAndSave(BriefingType.DAILY);
  }

  List<TbInvestmentBriefing> findAll();

  Optional<TbInvestmentBriefing> findById(Long briefingId);

  Optional<TbInvestmentBriefing> findByDate(LocalDate briefingDate);

  TbInvestmentBriefing save(TbInvestmentBriefing briefing);

  void delete(Long briefingId);
}
