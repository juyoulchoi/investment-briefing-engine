package com.nanum.investment.repository;

import com.nanum.investment.domain.TbMktSent;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TbMktSentRepository extends JpaRepository<TbMktSent, Long> {
  Optional<TbMktSent> findByBaseDateAndMarketSnapshotCode(LocalDate date, String code);
}
