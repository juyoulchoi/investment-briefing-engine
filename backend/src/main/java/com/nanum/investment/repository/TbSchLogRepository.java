package com.nanum.investment.repository;

import com.nanum.investment.domain.TbSchLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbSchLogRepository extends JpaRepository<TbSchLog, Long> {
  Optional<TbSchLog> findTopByJobCodeOrderByStartDateTimeDesc(String jobCode);
}
