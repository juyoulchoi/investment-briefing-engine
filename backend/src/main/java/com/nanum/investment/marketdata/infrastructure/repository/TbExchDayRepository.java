package com.nanum.investment.marketdata.infrastructure.repository;

import com.nanum.investment.marketdata.domain.TbExchDay;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbExchDayRepository extends JpaRepository<TbExchDay, Long> {
  Optional<TbExchDay> findTopByBaseCurrencyCodeAndQuoteCurrencyCodeOrderByBaseDateDesc(
      String base, String quote);
}
