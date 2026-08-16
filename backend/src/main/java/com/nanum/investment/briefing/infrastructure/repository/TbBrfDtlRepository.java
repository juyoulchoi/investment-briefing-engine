package com.nanum.investment.briefing.infrastructure.repository;

import com.nanum.investment.briefing.domain.TbBrfDtl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbBrfDtlRepository extends JpaRepository<TbBrfDtl, Long> {
  List<TbBrfDtl> findByBrfIdOrderByDtlId(Long brfId);

  void deleteByBrfId(Long brfId);
}
