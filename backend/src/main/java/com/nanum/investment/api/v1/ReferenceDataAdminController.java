package com.nanum.investment.api.v1;

import com.nanum.investment.common.exception.*;
import com.nanum.investment.common.response.ApiResponse;
import com.nanum.investment.common.web.TraceIdUtils;
import com.nanum.investment.domain.*;
import com.nanum.investment.repository.*;
import com.nanum.investment.request.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reference")
@Transactional(readOnly=true)
public class ReferenceDataAdminController {
 private final TbIdxRepository indices; private final TbAcctRepository accounts; private final TbStkRepository stocks;
 public ReferenceDataAdminController(TbIdxRepository indices,TbAcctRepository accounts,TbStkRepository stocks){this.indices=indices;this.accounts=accounts;this.stocks=stocks;}
 public record IndexRow(Long indexId,String indexCode,String indexName,String indexEnglishName,IndexType indexType,String marketCode,String countryCode,String currencyCode,DataSourceCode dataSourceCode,String sourceSymbol,String defaultYn,String useYn){}
 public record AccountRow(Long accountId,AccountType accountType,String brokerCode,String brokerName,String maskedAccountNumber,String baseCurrencyCode,java.math.BigDecimal cashAmount,java.math.BigDecimal reservedCashAmount,java.math.BigDecimal targetCashWeight,Integer displaySequence,String useYn){}
 public record StockRow(Long stockId,String stockCode,String stockName,String stockEnglishName,String marketCode,String countryCode,String currencyCode,AssetType assetType,StockGrade stockGrade,Long baseIndexId,String sectorCode,String sectorName,String industryCode,String industryName,String regularBuyYn,String additionalBuyYn,String rebuyYn,String useYn){}
 @GetMapping("/indices") public ApiResponse<List<IndexRow>> indexList(HttpServletRequest r){return ok(indices.findAll().stream().filter(x->"N".equals(x.getDeleteYn())).map(this::row).toList(),r);}
 @GetMapping("/accounts") public ApiResponse<List<AccountRow>> accountList(HttpServletRequest r){return ok(accounts.findAll().stream().filter(x->"N".equals(x.getDeleteYn())).map(this::row).toList(),r);}
 @PostMapping("/accounts") @Transactional public ApiResponse<AccountRow> createAccount(@Valid @RequestBody AccountSaveRequest b,HttpServletRequest r){if(accounts.findByAccountTypeAndDeleteYn(b.accountType(),"N").isPresent())throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);TbAcct x=new TbAcct();apply(x,b);x.setCreatedUserId("ADMIN");x.setUpdatedUserId("ADMIN");return ok(row(accounts.save(x)),r);}
 @PutMapping("/accounts/{id}") @Transactional public ApiResponse<AccountRow> updateAccount(@PathVariable Long id,@Valid @RequestBody AccountSaveRequest b,HttpServletRequest r){TbAcct x=accounts.findById(id).filter(v->"N".equals(v.getDeleteYn())).orElseThrow(()->new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));accounts.findByAccountTypeAndDeleteYn(b.accountType(),"N").filter(v->!v.getAccountId().equals(id)).ifPresent(v->{throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);});apply(x,b);x.setUpdatedUserId("ADMIN");return ok(row(accounts.save(x)),r);}
 @GetMapping("/stocks") public ApiResponse<List<StockRow>> stockList(HttpServletRequest r){return ok(stocks.findAll().stream().filter(x->"N".equals(x.getDeleteYn())).map(this::row).toList(),r);}
 private void apply(TbAcct x,AccountSaveRequest b){x.setAccountType(b.accountType());x.setBrokerCode(b.brokerCode());x.setBrokerName(b.brokerName());x.setMaskedAccountNumber(b.maskedAccountNumber());x.setBaseCurrencyCode(b.baseCurrencyCode());x.setCashAmount(b.cashAmount());x.setReservedCashAmount(b.reservedCashAmount());x.setTargetCashWeight(b.targetCashWeight());x.setDisplaySequence(b.displaySequence());x.setUseYn(nvl(b.useYn(),"Y"));x.setDeleteYn("N");}
 private IndexRow row(TbIdx x){return new IndexRow(x.getIndexId(),x.getIndexCode(),x.getIndexName(),x.getIndexEnglishName(),x.getIndexType(),x.getMarketCode(),x.getCountryCode(),x.getCurrencyCode(),x.getDataSourceCode(),x.getSourceSymbol(),x.getDefaultYn(),x.getUseYn());}
 private AccountRow row(TbAcct x){return new AccountRow(x.getAccountId(),x.getAccountType(),x.getBrokerCode(),x.getBrokerName(),x.getMaskedAccountNumber(),x.getBaseCurrencyCode(),x.getCashAmount(),x.getReservedCashAmount(),x.getTargetCashWeight(),x.getDisplaySequence(),x.getUseYn());}
 private StockRow row(TbStk x){return new StockRow(x.getStockId(),x.getStockCode(),x.getStockName(),x.getStockEnglishName(),x.getMarketCode(),x.getCountryCode(),x.getCurrencyCode(),x.getAssetType(),x.getStockGrade(),x.getBaseIndex()==null?null:x.getBaseIndex().getIndexId(),x.getSectorCode(),x.getSectorName(),x.getIndustryCode(),x.getIndustryName(),x.getRegularBuyYn(),x.getAdditionalBuyYn(),x.getRebuyYn(),x.getUseYn());}
 private String nvl(String v,String d){return v==null?d:v;} private <T> ApiResponse<T> ok(T d,HttpServletRequest r){return ApiResponse.success(d,TraceIdUtils.resolve(r));}
}

