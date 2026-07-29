package com.nanum.investment.service;

import com.nanum.investment.domain.TbInvestmentBriefing;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvestmentBriefingService {
    Long generateAndSave();
    List<TbInvestmentBriefing> findAll();
    Optional<TbInvestmentBriefing> findById(Long briefingId);
    Optional<TbInvestmentBriefing> findByDate(LocalDate briefingDate);
    TbInvestmentBriefing save(TbInvestmentBriefing briefing);
    void delete(Long briefingId);
}

