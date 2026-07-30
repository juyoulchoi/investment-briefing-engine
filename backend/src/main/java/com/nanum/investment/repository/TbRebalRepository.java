package com.nanum.investment.repository;

import com.nanum.investment.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TbRebalRepository extends JpaRepository<TbRebal,Long> {
 Optional<TbRebal> findTopByAccount_AccountIdAndRebalanceTypeAndLatestYnOrderByBaseDateDescCalculationSequenceDesc(
   Long accountId, RebalanceType rebalanceType, String latestYn);
}
