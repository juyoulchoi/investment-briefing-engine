package com.nanum.investment.common.exception;
import lombok.Getter;
@Getter public class BusinessException extends RuntimeException {
 private final ErrorCode errorCode;
 public BusinessException(ErrorCode code){super(code.getMessage());this.errorCode=code;}
 public BusinessException(ErrorCode code,String message){super(message);this.errorCode=code;}
}
