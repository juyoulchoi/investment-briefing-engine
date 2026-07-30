package com.nanum.investment.scheduler;
import com.nanum.investment.domain.*; import com.nanum.investment.repository.TbErrLogRepository; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.OffsetDateTime;
@Service public class SchedulerRecoveryService {
 private final TbErrLogRepository errors; public SchedulerRecoveryService(TbErrLogRepository errors){this.errors=errors;}
 @Transactional public TbErrLog resolve(Long errorLogId,String userId,String memo){
  TbErrLog error=errors.findById(errorLogId).orElseThrow(()->new IllegalArgumentException("오류 로그를 찾을 수 없습니다."));
  if("Y".equals(error.getResolvedYn()))return error;
  error.setResolvedYn("Y");error.setResolvedDateTime(OffsetDateTime.now());error.setResolvedUserId(userId);error.setResolutionMemo(memo);return errors.save(error);
 }
}

