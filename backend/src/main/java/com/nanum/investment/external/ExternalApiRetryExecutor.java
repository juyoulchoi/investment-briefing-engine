package com.nanum.investment.external;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExternalApiRetryExecutor {
 private static final int MAXIMUM_ATTEMPTS=4;
 private static final List<Duration> DELAYS=List.of(Duration.ofSeconds(1),Duration.ofSeconds(3),Duration.ofSeconds(10));
 public <T> T execute(Callable<T> action){
  Throwable last=null;
  for(int attempt=1;attempt<=MAXIMUM_ATTEMPTS;attempt++)try{return action.call();}catch(Throwable error){
   last=error;if(attempt==MAXIMUM_ATTEMPTS||!ExternalHttpRetry.isRetryable(error))throw propagate(error);
   Duration retryAfter=ExternalHttpRetry.retryAfter(error);sleep(retryAfter!=null?retryAfter:jitter(DELAYS.get(attempt-1)));
  }
  throw propagate(last);
 }
 Duration jitter(Duration base){double factor=ThreadLocalRandom.current().nextDouble(0.8,1.2);return Duration.ofMillis(Math.round(base.toMillis()*factor));}
 protected void sleep(Duration delay){try{Thread.sleep(delay.toMillis());}catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException("재시도 대기가 중단되었습니다.",error);}}
 private RuntimeException propagate(Throwable error){return error instanceof RuntimeException runtime?runtime:new IllegalStateException(error);}
}
