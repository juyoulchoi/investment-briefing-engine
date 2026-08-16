package com.nanum.investment.marketdata.infrastructure.repository;

import com.nanum.investment.marketdata.domain.TbIdxDay;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbIdxDayRepository extends JpaRepository<TbIdxDay, Long> {
  Optional<TbIdxDay> findTopByIndex_IndexIdOrderByTradeDateDesc(Long indexId);
}
