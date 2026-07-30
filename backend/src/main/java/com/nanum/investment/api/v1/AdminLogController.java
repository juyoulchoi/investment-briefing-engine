package com.nanum.investment.api.v1;

import com.nanum.investment.common.exception.*;
import com.nanum.investment.common.response.*;
import com.nanum.investment.common.web.TraceIdUtils;
import com.nanum.investment.domain.*;
import com.nanum.investment.repository.*;
import com.nanum.investment.scheduler.SchedulerRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration", description = "스케줄·외부 API·오류 로그")
public class AdminLogController {
    private final TbSchLogRepository schedulerLogs;
    private final TbApiLogRepository apiLogs;
    private final TbErrLogRepository errorLogs;
    private final SchedulerRecoveryService recovery;

    public AdminLogController(TbSchLogRepository s, TbApiLogRepository a, TbErrLogRepository e,
            SchedulerRecoveryService r) {
        schedulerLogs = s;
        apiLogs = a;
        errorLogs = e;
        recovery = r;
    }

    @GetMapping("/scheduler-logs")
    @Operation(summary = "스케줄러 로그 목록")
    public ApiResponse<PageResponse<TbSchLog>> scheduler(Pageable p, HttpServletRequest r) {
        return ok(PageResponse.from(schedulerLogs.findAll(p)), r);
    }

    @GetMapping("/scheduler-logs/{id}")
    public ApiResponse<TbSchLog> scheduler(@PathVariable Long id, HttpServletRequest r) {
        return ok(schedulerLogs.findById(id).orElseThrow(this::notFound), r);
    }

    @GetMapping("/api-logs")
    @Operation(summary = "외부 API 로그 목록")
    public ApiResponse<PageResponse<TbApiLog>> api(Pageable p, HttpServletRequest r) {
        return ok(PageResponse.from(apiLogs.findAll(p)), r);
    }

    @GetMapping("/api-logs/{id}")
    public ApiResponse<TbApiLog> api(@PathVariable Long id, HttpServletRequest r) {
        return ok(apiLogs.findById(id).orElseThrow(this::notFound), r);
    }

    @GetMapping("/error-logs")
    @Operation(summary = "오류 로그 목록")
    public ApiResponse<PageResponse<TbErrLog>> errors(Pageable p, HttpServletRequest r) {
        return ok(PageResponse.from(errorLogs.findAll(p)), r);
    }

    @PostMapping("/error-logs/{id}/resolve")
    public ApiResponse<TbErrLog> resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest body,
            HttpServletRequest r) {
        return ok(recovery.resolve(id, body.userId(), body.memo()), r);
    }

    @PostMapping("/error-logs/{id}/reopen")
    @Transactional
    public ApiResponse<TbErrLog> reopen(@PathVariable Long id, HttpServletRequest r) {
        TbErrLog error = errorLogs.findById(id).orElseThrow(this::notFound);
        error.setResolvedYn("N");
        error.setResolvedDateTime(null);
        error.setResolvedUserId(null);
        error.setResolutionMemo(null);
        return ok(errorLogs.save(error), r);
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest req) {
        return ApiResponse.success(data, TraceIdUtils.resolve(req));
    }

    public record ResolveRequest(@NotBlank @Size(max = 100) String userId, @NotBlank @Size(max = 2000) String memo) {
    }
}
