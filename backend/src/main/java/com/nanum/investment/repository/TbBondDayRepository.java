package com.nanum.investment.repository;

import com.nanum.investment.domain.TbBondDay;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbBondDayRepository extends JpaRepository<TbBondDay, Long> {
  Optional<TbBondDay> findTopByBondCodeOrderByBaseDateDesc(String bondCode);
}
