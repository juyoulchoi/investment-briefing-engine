package com.nanum.investment.calculation;
import org.springframework.stereotype.Component; import java.math.*; import java.util.*;
@Component public class AdditionalBuyAllocator {
 public List<Allocation> allocate(BigDecimal budget,List<Candidate> candidates){
  if(budget==null||budget.signum()<0)throw new IllegalArgumentException("추가매수 예산은 0 이상이어야 합니다.");
  BigDecimal remaining=budget; List<Allocation> out=new ArrayList<>();
  List<Candidate> sorted=candidates==null?List.of():candidates.stream().filter(Candidate::eligible).sorted(Comparator.comparing(Candidate::priorityScore).reversed()).toList();
  for(Candidate c:sorted){BigDecimal amount=min(nonNegative(c.maximumAmount()),nonNegative(c.weightGapAmount()),remaining);out.add(new Allocation(c.stockId(),amount));remaining=remaining.subtract(amount);}
  return List.copyOf(out);
 }
 private BigDecimal nonNegative(BigDecimal v){return v==null?BigDecimal.ZERO:v.max(BigDecimal.ZERO);}
 private BigDecimal min(BigDecimal... v){return Arrays.stream(v).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);}
 public record Candidate(Long stockId,boolean eligible,BigDecimal priorityScore,BigDecimal maximumAmount,BigDecimal weightGapAmount){}
 public record Allocation(Long stockId,BigDecimal amount){}
}
