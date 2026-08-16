package com.nanum.investment.rebalance.infrastructure.repository;

import com.nanum.investment.rebalance.domain.TbRebalItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRebalItemRepository extends JpaRepository<TbRebalItem, Long> {
  List<TbRebalItem> findAllByRebalance_RebalanceIdOrderByPriorityNumberAsc(Long rebalanceId);
}
