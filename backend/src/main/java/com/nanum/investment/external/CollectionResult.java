package com.nanum.investment.external;
import com.nanum.investment.domain.DataStatus; import java.util.List;
public record CollectionResult<T>(boolean success,T data,DataStatus dataStatus,int savedCount,List<String> warnings,String errorMessage){
 public static <T> CollectionResult<T> success(T data,int count,List<String> warnings){return new CollectionResult<>(true,data,warnings==null||warnings.isEmpty()?DataStatus.FRESH:DataStatus.PARTIAL,count,warnings==null?List.of():List.copyOf(warnings),null);}
 public static <T> CollectionResult<T> failure(String message){return new CollectionResult<>(false,null,DataStatus.ERROR,0,List.of(),message);}
}
