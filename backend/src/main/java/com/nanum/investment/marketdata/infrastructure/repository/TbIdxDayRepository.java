package com.nanum.investment.marketdata.infrastructure.repository;

import com.nanum.investment.marketdata.domain.TbIdxDay;
import com.nanum.investment.marketdata.domain.TbIdxDayId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbIdxDayRepository extends JpaRepository<TbIdxDay, TbIdxDayId> {
  Optional<TbIdxDay> findTopByIndex_IndexCodeOrderByTradeDateDesc(String indexCode);
}
