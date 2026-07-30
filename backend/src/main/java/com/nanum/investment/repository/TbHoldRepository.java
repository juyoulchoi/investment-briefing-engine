package com.nanum.investment.repository;
import com.nanum.investment.domain.TbHold; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TbHoldRepository extends JpaRepository<TbHold,Long>{ Optional<TbHold> findByAccount_AccountIdAndStock_StockIdAndDeleteYn(Long accountId,Long stockId,String deleteYn); List<TbHold> findAllByAccount_AccountIdAndUseYnAndDeleteYn(Long accountId,String useYn,String deleteYn); }
