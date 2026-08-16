package com.nanum.investment.common.infrastructure.repository;

import com.nanum.investment.common.domain.TbApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbApiLogRepository extends JpaRepository<TbApiLog, Long> {}
