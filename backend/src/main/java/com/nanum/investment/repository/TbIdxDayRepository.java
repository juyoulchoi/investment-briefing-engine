package com.nanum.investment.repository; import com.nanum.investment.domain.TbIdxDay; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface TbIdxDayRepository extends JpaRepository<TbIdxDay,Long>{Optional<TbIdxDay> findTopByIndex_IndexIdOrderByTradeDateDesc(Long indexId);}
