package com.nanum.investment.repository;

import com.nanum.investment.domain.TbDecChg;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbDecChgRepository extends JpaRepository<TbDecChg, Long> {
  List<TbDecChg> findAllByStockDecision_StockDecisionIdOrderByDecisionChangeIdAsc(
      Long stockDecisionId);
}
