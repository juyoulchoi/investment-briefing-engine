package com.nanum.investment.marketdata.infrastructure.repository;

import com.nanum.investment.marketdata.domain.TbPrcDay;
import com.nanum.investment.marketdata.domain.TbPrcDayId;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbPrcDayRepository extends JpaRepository<TbPrcDay, TbPrcDayId> {
  Optional<TbPrcDay> findTopByStock_StockIdOrderByTradeDateDesc(Long stockId);

  List<TbPrcDay> findAllByStock_StockIdAndTradeDateBetweenOrderByTradeDateAsc(
      Long stockId, LocalDate start, LocalDate end);
}
