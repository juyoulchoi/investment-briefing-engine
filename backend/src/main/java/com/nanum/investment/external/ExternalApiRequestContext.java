package com.nanum.investment.external;
import java.time.LocalDate;
public record ExternalApiRequestContext(String traceId,String apiGroupCode,String apiName,LocalDate baseDate,Integer attemptNumber){}
