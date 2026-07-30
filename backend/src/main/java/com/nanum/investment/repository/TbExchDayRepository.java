package com.nanum.investment.repository; import com.nanum.investment.domain.TbExchDay; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface TbExchDayRepository extends JpaRepository<TbExchDay,Long>{Optional<TbExchDay> findTopByBaseCurrencyCodeAndQuoteCurrencyCodeOrderByBaseDateDesc(String base,String quote);}
