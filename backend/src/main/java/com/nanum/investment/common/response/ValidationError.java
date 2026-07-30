package com.nanum.investment.common.response;
public record ValidationError(String field,Object rejectedValue,String message){}
