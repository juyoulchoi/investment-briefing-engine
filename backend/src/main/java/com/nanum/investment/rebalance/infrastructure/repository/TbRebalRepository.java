package com.nanum.investment.rebalance.infrastructure.repository;

import com.nanum.investment.rebalance.domain.RebalanceType;
import com.nanum.investment.rebalance.domain.TbRebal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRebalRepository extends JpaRepository<TbRebal, Long> {
  Optional<TbRebal>
      findTopByAccount_AccountIdAndRebalanceTypeAndLatestYnOrderByBaseDateDescCalculationSequenceDesc(
          Long accountId, RebalanceType rebalanceType, String latestYn);
}
