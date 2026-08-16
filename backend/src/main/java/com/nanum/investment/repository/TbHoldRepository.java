package com.nanum.investment.repository;

import com.nanum.investment.domain.TbHold;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbHoldRepository extends JpaRepository<TbHold, Long> {
  Optional<TbHold> findByAccount_AccountIdAndStock_StockIdAndDeleteYn(
      Long accountId, Long stockId, String deleteYn);

  List<TbHold> findAllByAccount_AccountIdAndUseYnAndDeleteYn(
      Long accountId, String useYn, String deleteYn);
}
