package com.nanum.investment.marketdata.infrastructure.repository;

import com.nanum.investment.marketdata.domain.TbIdx;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbIdxRepository extends JpaRepository<TbIdx, String> {
  Optional<TbIdx> findByIndexCodeAndDeleteYn(String indexCode, String deleteYn);

  List<TbIdx> findAllByUseYnAndDeleteYnOrderByIndexNameAsc(String useYn, String deleteYn);
}
