package com.nanum.investment.repository;

import com.nanum.investment.domain.TbPrcDay;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbPrcDayRepository extends JpaRepository<TbPrcDay, Long> {
  Optional<TbPrcDay> findTopByStock_StockIdOrderByTradeDateDesc(Long stockId);

  List<TbPrcDay> findAllByStock_StockIdAndTradeDateBetweenOrderByTradeDateAsc(
      Long stockId, LocalDate start, LocalDate end);
}
