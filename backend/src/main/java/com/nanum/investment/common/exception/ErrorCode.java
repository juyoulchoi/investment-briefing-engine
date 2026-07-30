package com.nanum.investment.common.exception;
import lombok.Getter; import org.springframework.http.HttpStatus;
@Getter public enum ErrorCode {
 INVALID_REQUEST(HttpStatus.BAD_REQUEST,"COMMON-400-001","요청값이 올바르지 않습니다."),
 RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,"COMMON-404-001","대상을 찾을 수 없습니다."),
 DUPLICATE_RESOURCE(HttpStatus.CONFLICT,"COMMON-409-001","이미 존재하는 데이터입니다."),
 ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND,"ACCOUNT-404-001","계좌를 찾을 수 없습니다."),
 STOCK_NOT_FOUND(HttpStatus.NOT_FOUND,"STOCK-404-001","종목을 찾을 수 없습니다."),
 INSUFFICIENT_CASH(HttpStatus.CONFLICT,"CASH-409-001","사용 가능한 현금이 부족합니다."),
 INVALID_REBALANCE_STATE(HttpStatus.CONFLICT,"REBAL-409-001","현재 상태에서는 리밸런싱 작업을 수행할 수 없습니다."),
 EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY,"API-502-001","외부 API 호출에 실패했습니다."),
 INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"COMMON-500-001","서버 내부 오류가 발생했습니다.");
 private final HttpStatus httpStatus; private final String code; private final String message;
 ErrorCode(HttpStatus status,String code,String message){this.httpStatus=status;this.code=code;this.message=message;}
}
