package com.nanum.investment.response;
import com.nanum.investment.domain.AccountType; import java.math.BigDecimal;
public record AccountApiResponse(Long accountId,String accountCode,String accountName,AccountType accountType,String baseCurrencyCode,
 BigDecimal cashAmount,BigDecimal reservedCashAmount,BigDecimal targetCashWeight,String useYn){}
