package com.nanum.investment.repository;
import com.nanum.investment.domain.TbIdx; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TbIdxRepository extends JpaRepository<TbIdx,Long>{ Optional<TbIdx> findByIndexCodeAndDeleteYn(String indexCode,String deleteYn); List<TbIdx> findAllByUseYnAndDeleteYnOrderByIndexNameAsc(String useYn,String deleteYn); }
