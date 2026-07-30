package com.nanum.investment.repository;
import com.nanum.investment.domain.TbSchLog; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface TbSchLogRepository extends JpaRepository<TbSchLog,Long>{Optional<TbSchLog> findTopByJobCodeOrderByStartDateTimeDesc(String jobCode);}
