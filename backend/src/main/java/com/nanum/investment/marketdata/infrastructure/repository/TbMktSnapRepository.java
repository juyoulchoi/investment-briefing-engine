package com.nanum.investment.marketdata.infrastructure.repository;

import com.nanum.investment.marketdata.domain.DataStatus;
import com.nanum.investment.marketdata.domain.TbMktSnap;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbMktSnapRepository extends JpaRepository<TbMktSnap, Long> {
  Optional<TbMktSnap> findByBaseDateAndMarketSnapshotCode(
      LocalDate baseDate, String marketSnapshotCode);

  Optional<TbMktSnap> findTopByMarketSnapshotCodeAndDataStatusOrderByBaseDateDesc(
      String code, DataStatus status);
}
