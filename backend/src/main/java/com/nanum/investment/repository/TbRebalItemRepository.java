package com.nanum.investment.repository;

import com.nanum.investment.domain.TbRebalItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRebalItemRepository extends JpaRepository<TbRebalItem, Long> {
  List<TbRebalItem> findAllByRebalance_RebalanceIdOrderByPriorityNumberAsc(Long rebalanceId);
}
