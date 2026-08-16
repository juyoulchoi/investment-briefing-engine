package com.nanum.investment.repository;

import com.nanum.investment.domain.TbStkDec;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbStkDecRepository extends JpaRepository<TbStkDec, Long> {
  List<TbStkDec> findAllByInvestmentDecision_InvestmentDecisionIdOrderByPriorityNumberAsc(
      Long investmentDecisionId);

  Optional<TbStkDec>
      findFirstByAccount_AccountIdAndStock_StockIdOrderByInvestmentDecision_BaseDateDescInvestmentDecision_CalculationSequenceDesc(
          Long accountId, Long stockId);
}
