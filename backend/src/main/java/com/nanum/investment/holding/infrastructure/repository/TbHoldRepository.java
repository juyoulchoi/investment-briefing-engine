package com.nanum.investment.holding.infrastructure.repository;

import com.nanum.investment.holding.domain.TbHold;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbHoldRepository extends JpaRepository<TbHold, Long> {
  Optional<TbHold> findByAccount_AccountIdAndStock_StockIdAndDeleteYn(
      Long accountId, Long stockId, String deleteYn);

  List<TbHold> findAllByAccount_AccountIdAndUseYnAndDeleteYn(
      Long accountId, String useYn, String deleteYn);
}
