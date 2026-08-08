package com.nanum.investment.response;
import com.nanum.investment.domain.*; 
public record StockApiResponse(Long stockId,String stockCode,String stockName,String marketCode,String countryCode,
 String currencyCode,AssetType assetType,StockGrade stockGrade,String sectorName,String useYn){}

