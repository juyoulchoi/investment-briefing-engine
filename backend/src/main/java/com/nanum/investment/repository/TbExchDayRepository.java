package com.nanum.investment.repository;

import com.nanum.investment.domain.TbExchDay;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbExchDayRepository extends JpaRepository<TbExchDay, Long> {
  Optional<TbExchDay> findTopByBaseCurrencyCodeAndQuoteCurrencyCodeOrderByBaseDateDesc(
      String base, String quote);
}
