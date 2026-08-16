package com.nanum.investment.common.infrastructure.repository;

import com.nanum.investment.common.domain.TbErrLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbErrLogRepository extends JpaRepository<TbErrLog, Long> {}
