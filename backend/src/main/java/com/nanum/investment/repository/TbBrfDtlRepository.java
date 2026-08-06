package com.nanum.investment.repository;

import com.nanum.investment.domain.TbBrfDtl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TbBrfDtlRepository extends JpaRepository<TbBrfDtl, Long> {
    List<TbBrfDtl> findByBrfIdOrderByDtlId(Long brfId);
    void deleteByBrfId(Long brfId);
}
