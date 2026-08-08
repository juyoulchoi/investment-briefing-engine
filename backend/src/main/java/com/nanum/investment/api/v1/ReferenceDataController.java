package com.nanum.investment.api.v1;

import com.nanum.investment.common.exception.*;
import com.nanum.investment.common.response.*;
import com.nanum.investment.common.web.TraceIdUtils;
import com.nanum.investment.domain.*;
import com.nanum.investment.repository.*;
import com.nanum.investment.request.*;
import com.nanum.investment.response.*;
import com.nanum.investment.service.HoldingManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Transactional(readOnly = true)
@Tag(name = "Reference Data", description = "계좌·종목·보유종목 조회 API")
public class ReferenceDataController {
    private final TbAcctRepository accounts;
    private final TbStkRepository stocks;
    private final TbHoldRepository holdings;
    private final HoldingManagementService holdingManagement;

    public ReferenceDataController(TbAcctRepository accounts, TbStkRepository stocks, TbHoldRepository holdings,
            HoldingManagementService holdingManagement) {
        this.accounts = accounts;
        this.stocks = stocks;
        this.holdings = holdings;
        this.holdingManagement = holdingManagement;
    }

    @GetMapping("/accounts")
    @Operation(summary = "계좌 목록")
    public ApiResponse<PageResponse<AccountApiResponse>> accounts(Pageable pageable, HttpServletRequest req) {
        List<AccountApiResponse> rows = accounts.findAllByUseYnAndDeleteYnOrderByDisplaySequenceAsc("Y", "N").stream()
                .map(this::account).toList();
        return ok(page(rows, pageable), req);
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "계좌 상세")
    public ApiResponse<AccountApiResponse> account(@PathVariable Long id, HttpServletRequest req) {
        return ok(account(accounts.findById(id).filter(a -> "N".equals(a.getDeleteYn()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))), req);
    }

    @GetMapping("/stocks")
    @Operation(summary = "종목 목록")
    public ApiResponse<PageResponse<StockApiResponse>> stocks(Pageable pageable, HttpServletRequest req) {
        return ok(page(stocks.findAllByUseYnAndDeleteYnOrderByStockNameAsc("Y", "N").stream().map(this::stock).toList(),
                pageable), req);
    }

    @GetMapping("/stocks/{id}")
    @Operation(summary = "종목 상세")
    public ApiResponse<StockApiResponse> stock(@PathVariable Long id, HttpServletRequest req) {
        return ok(stock(stocks.findById(id).filter(s -> "N".equals(s.getDeleteYn()))
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND))), req);
    }

    @GetMapping("/holdings")
    @Operation(summary = "보유종목 목록")
    public ApiResponse<PageResponse<HoldingApiResponse>> holdings(@RequestParam Long accountId, Pageable pageable,
            HttpServletRequest req) {
        return ok(page(holdings.findAllByAccount_AccountIdAndUseYnAndDeleteYn(accountId, "Y", "N").stream()
                .map(this::holding).toList(), pageable), req);
    }

    @PatchMapping("/accounts/{accountId}/holdings")
    @Transactional
    @Operation(summary = "계좌 보유수량 및 평단가 일괄 수정")
    public ApiResponse<List<HoldingApiResponse>> updateAccountHoldings(@PathVariable Long accountId,
            @Valid @RequestBody HoldingBatchUpdateRequest body, HttpServletRequest req) {
        return ok(holdingManagement.updateAccount(accountId, body).stream().map(this::holding).toList(), req);
    }

    @PatchMapping("/holdings/{id}")
    @Transactional
    @Operation(summary = "보유수량 및 평단가 수정")
    public ApiResponse<HoldingApiResponse> updateHolding(@PathVariable Long id,
            @Valid @RequestBody HoldingUpdateRequest body, HttpServletRequest req) {
        return ok(holding(holdingManagement.update(id, body)), req);
    }

    @GetMapping("/holdings/{id}")
    @Operation(summary = "보유종목 상세")
    public ApiResponse<HoldingApiResponse> holding(@PathVariable Long id, HttpServletRequest req) {
        return ok(holding(holdings.findById(id).filter(h -> "N".equals(h.getDeleteYn()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))), req);
    }

    private AccountApiResponse account(TbAcct a) {
        return new AccountApiResponse(a.getAccountId(), a.getAccountType().name(), accountName(a.getAccountType()),
                a.getAccountType(), a.getBaseCurrencyCode(), a.getCashAmount(), a.getReservedCashAmount(),
                a.getTargetCashWeight(), a.getUseYn());
    }

    private StockApiResponse stock(TbStk s) {
        return new StockApiResponse(s.getStockId(), s.getStockCode(), s.getStockName(),
                s.getMarketCode(), s.getCountryCode(), s.getCurrencyCode(), s.getAssetType(), s.getStockGrade(),
                s.getSectorName(), s.getUseYn());
    }

    private HoldingApiResponse holding(TbHold h) {
        return new HoldingApiResponse(h.getHoldingId(), h.getAccount().getAccountId(),
                accountName(h.getAccount().getAccountType()), h.getStock().getStockId(), h.getStock().getStockCode(),
                h.getStock().getStockName(), h.getHoldingQuantity(), h.getAveragePrice(), h.getCurrentPrice(),
                h.getEvaluationAmount(), h.getProfitLossRate(), h.getTargetWeight(), h.getCurrentWeight(),
                h.getWeightStatus(), h.getHoldingStatus());
    }

    private String accountName(AccountType type) {
        return switch (type) {
            case DOMESTIC -> "국내계좌";
            case OVERSEAS -> "해외계좌";
            case ISA -> "ISA계좌";
            case PENSION -> "연금계좌";
        };
    }

    private <T> PageResponse<T> page(List<T> rows, Pageable p) {
        int from = Math.min((int) p.getOffset(), rows.size()), to = Math.min(from + p.getPageSize(), rows.size());
        return PageResponse.from(new PageImpl<>(rows.subList(from, to), p, rows.size()));
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest req) {
        return ApiResponse.success(data, TraceIdUtils.resolve(req));
    }
}

