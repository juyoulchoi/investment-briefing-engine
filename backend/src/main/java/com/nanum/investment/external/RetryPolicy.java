package com.nanum.investment.external;
import java.time.Duration;
public record RetryPolicy(int maximumAttempts,Duration initialDelay,Duration maximumDelay){
 public RetryPolicy{if(maximumAttempts<1)throw new IllegalArgumentException("최대 시도 횟수는 1 이상이어야 합니다.");}
 public static RetryPolicy externalApiDefault(){return new RetryPolicy(4,Duration.ofMillis(250),Duration.ofSeconds(3));}
}
