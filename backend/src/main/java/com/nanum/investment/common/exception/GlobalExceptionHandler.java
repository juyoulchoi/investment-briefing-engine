package com.nanum.investment.common.exception;
import com.nanum.investment.common.response.*; import com.nanum.investment.common.web.TraceIdUtils; import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
import java.util.List;
@Slf4j @RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(BusinessException.class) ResponseEntity<ApiResponse<Void>> business(BusinessException ex,HttpServletRequest req){
  ErrorCode c=ex.getErrorCode(); return ResponseEntity.status(c.getHttpStatus()).body(ApiResponse.failure(new ErrorResponse(c.getCode(),ex.getMessage(),List.of()),TraceIdUtils.resolve(req)));
 }
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException ex,HttpServletRequest req){
  List<ValidationError> errors=ex.getBindingResult().getFieldErrors().stream().map(e->new ValidationError(e.getField(),e.getRejectedValue(),e.getDefaultMessage())).toList();
  ErrorCode c=ErrorCode.INVALID_REQUEST; return ResponseEntity.badRequest().body(ApiResponse.failure(new ErrorResponse(c.getCode(),c.getMessage(),errors),TraceIdUtils.resolve(req)));
 }
 @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> unknown(Exception ex,HttpServletRequest req){
  String id=TraceIdUtils.resolve(req); log.error("Unhandled exception. traceId={}",id,ex); ErrorCode c=ErrorCode.INTERNAL_SERVER_ERROR;
  return ResponseEntity.status(c.getHttpStatus()).body(ApiResponse.failure(new ErrorResponse(c.getCode(),c.getMessage(),List.of()),id));
 }
}
