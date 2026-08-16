package com.nanum.investment.regularbuy.infrastructure.repository;

import com.nanum.investment.regularbuy.domain.TbRegBuy;
import com.nanum.investment.regularbuy.domain.TbRegBuyId;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbRegBuyRepository extends JpaRepository<TbRegBuy, TbRegBuyId> {
  Optional<TbRegBuy> findByAccount_AccountIdAndStock_StockIdAndDeleteYn(
      Long accountId, Long stockId, String deleteYn);
}
