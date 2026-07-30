package com.nanum.investment.repository;
import com.nanum.investment.domain.TbRegBuy; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TbRegBuyRepository extends JpaRepository<TbRegBuy,Long>{ Optional<TbRegBuy> findByAccount_AccountIdAndStock_StockIdAndDeleteYn(Long accountId,Long stockId,String deleteYn); }
