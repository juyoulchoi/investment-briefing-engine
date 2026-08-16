package com.nanum.investment.repository;

import com.nanum.investment.domain.TbStk;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbStkRepository extends JpaRepository<TbStk, Long> {
  Optional<TbStk> findByStockCodeAndDeleteYn(String stockCode, String deleteYn);

  List<TbStk> findAllByUseYnAndDeleteYnOrderByStockNameAsc(String useYn, String deleteYn);
}
