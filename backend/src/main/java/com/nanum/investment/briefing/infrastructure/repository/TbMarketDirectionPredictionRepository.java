package com.nanum.investment.briefing.infrastructure.repository;

import com.nanum.investment.briefing.domain.TbMarketDirectionPrediction;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbMarketDirectionPredictionRepository extends JpaRepository<TbMarketDirectionPrediction, Long> {
  Optional<TbMarketDirectionPrediction> findTopByBaseDateAndLatestYnOrderByCalculationSequenceDesc(LocalDate baseDate, String latestYn);
}
