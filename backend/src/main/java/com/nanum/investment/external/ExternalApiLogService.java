package com.nanum.investment.external;

import com.nanum.investment.domain.TbApiLog;
import com.nanum.investment.repository.TbApiLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class ExternalApiLogService {
 private final TbApiLogRepository repository; private final ApiLogMasker masker;
 public ExternalApiLogService(TbApiLogRepository repository,ApiLogMasker masker){this.repository=repository;this.masker=masker;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public TbApiLog save(String traceId,String group,String name,String method,String url,String requestBody,Integer status,
                      String responseBody,boolean success,int retryCount,OffsetDateTime requestedAt,String error){
  OffsetDateTime respondedAt=OffsetDateTime.now();
  return repository.save(TbApiLog.builder().traceId(traceId).apiGroupCode(group).apiName(name).httpMethod(method)
   .requestUrl(masker.maskAndLimit(url)).requestBody(masker.maskAndLimit(requestBody)).httpStatusCode(status)
   .responseBody(masker.maskAndLimit(responseBody)).successYn(success?"Y":"N").errorMessage(masker.maskAndLimit(error))
   .elapsedMilliseconds(Duration.between(requestedAt,respondedAt).toMillis()).retryCount(retryCount)
   .requestDateTime(requestedAt).responseDateTime(respondedAt).build());
 }
}
