package com.nanum.investment.repository;

import com.nanum.investment.domain.TbInvBrf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TbInvBrfRepository extends JpaRepository<TbInvBrf, Long> {
    Optional<TbInvBrf> findTopByBaseDateAndLatestYnOrderByCalculationSequenceDesc(LocalDate baseDate, String latestYn);
}

