package com.nanum.investment.repository;

import com.nanum.investment.domain.TbInvestmentBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface TbInvestmentBriefingRepository extends JpaRepository<TbInvestmentBriefing, Long> {
    Optional<TbInvestmentBriefing> findByBriefingDate(LocalDate briefingDate);
}
