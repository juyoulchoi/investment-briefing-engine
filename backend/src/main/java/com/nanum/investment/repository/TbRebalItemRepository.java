package com.nanum.investment.repository;

import com.nanum.investment.domain.TbRebalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TbRebalItemRepository extends JpaRepository<TbRebalItem,Long> {
 List<TbRebalItem> findAllByRebalance_RebalanceIdOrderByPriorityNumberAsc(Long rebalanceId);
}
