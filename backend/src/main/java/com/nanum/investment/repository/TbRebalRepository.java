package com.nanum.investment.repository;

import com.nanum.investment.domain.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRebalRepository extends JpaRepository<TbRebal, Long> {
  Optional<TbRebal>
      findTopByAccount_AccountIdAndRebalanceTypeAndLatestYnOrderByBaseDateDescCalculationSequenceDesc(
          Long accountId, RebalanceType rebalanceType, String latestYn);
}
