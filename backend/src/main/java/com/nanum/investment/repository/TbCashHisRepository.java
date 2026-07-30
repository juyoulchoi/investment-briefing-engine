package com.nanum.investment.repository;
import com.nanum.investment.domain.TbCashHis; import org.springframework.data.jpa.repository.JpaRepository;
public interface TbCashHisRepository extends JpaRepository<TbCashHis,Long>{ boolean existsByIdempotencyKey(String idempotencyKey); }
