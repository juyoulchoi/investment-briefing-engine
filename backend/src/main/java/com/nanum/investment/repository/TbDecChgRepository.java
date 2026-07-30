package com.nanum.investment.repository;

import com.nanum.investment.domain.TbDecChg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TbDecChgRepository extends JpaRepository<TbDecChg, Long> {
    List<TbDecChg> findAllByStockDecision_StockDecisionIdOrderByDecisionChangeIdAsc(Long stockDecisionId);
}
