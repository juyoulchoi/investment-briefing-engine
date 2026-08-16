package com.nanum.investment.repository;

import com.nanum.investment.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRegBuyRepository extends JpaRepository<TbRegBuy, TbRegBuyId> {
  Optional<TbRegBuy> findByAccount_AccountIdAndStock_StockIdAndDeleteYn(
      Long accountId, Long stockId, String deleteYn);
}
