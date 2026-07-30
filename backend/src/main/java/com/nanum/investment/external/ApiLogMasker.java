package com.nanum.investment.external;
import org.springframework.stereotype.Component; import java.util.regex.Pattern;
@Component public class ApiLogMasker {
 private static final Pattern SECRET=Pattern.compile("(?i)(api[_-]?key|authorization|token|secret|account(?:number)?)([\"'\\s:=]+)([^&\\s,}\"']+)");
 private static final int LIMIT=8000;
 public String maskAndLimit(String value){if(value==null)return null;String masked=SECRET.matcher(value).replaceAll("$1$2***");return masked.length()<=LIMIT?masked:masked.substring(0,LIMIT)+"...[TRUNCATED]";}
}


