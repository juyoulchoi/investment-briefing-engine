package com.nanum.investment.repository; import com.nanum.investment.domain.TbBondDay; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface TbBondDayRepository extends JpaRepository<TbBondDay,Long>{Optional<TbBondDay> findTopByBondCodeOrderByBaseDateDesc(String bondCode);}
