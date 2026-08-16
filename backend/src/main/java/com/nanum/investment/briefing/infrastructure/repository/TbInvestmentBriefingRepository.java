package com.nanum.investment.briefing.infrastructure.repository;

import com.nanum.investment.briefing.domain.TbInvestmentBriefing;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbInvestmentBriefingRepository extends JpaRepository<TbInvestmentBriefing, Long> {
  Optional<TbInvestmentBriefing> findByBriefingDate(LocalDate briefingDate);
}
