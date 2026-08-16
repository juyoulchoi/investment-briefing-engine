package com.nanum.investment.holding.infrastructure.repository;

import com.nanum.investment.holding.domain.TbCashHis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbCashHisRepository extends JpaRepository<TbCashHis, Long> {
  boolean existsByIdempotencyKey(String idempotencyKey);
}
