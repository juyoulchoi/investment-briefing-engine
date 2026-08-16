package com.nanum.investment.marketdata.infrastructure.repository;

import com.nanum.investment.marketdata.domain.TbBondDay;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbBondDayRepository extends JpaRepository<TbBondDay, Long> {
  Optional<TbBondDay> findTopByBondCodeOrderByBaseDateDesc(String bondCode);
}
