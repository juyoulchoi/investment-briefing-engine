package com.nanum.investment.service;
import com.nanum.investment.domain.*; import org.springframework.stereotype.Service; import java.util.*;
@Service
public class MarketRegimeService {
 private final MarketSentimentService sentimentService; public MarketRegimeService(MarketSentimentService s){this.sentimentService=s;}
 public MarketAssessment assess(MarketSnapshot s){
  SentimentAssessment sentiment=sentimentService.assess(s); List<String> reasons=new ArrayList<>(); int score=50;
  score+=norm(s.earningsConfidenceScore(),50,20); score+=norm(s.liquidityScore(),50,15); score-=norm(sentiment.sentimentRiskScore(),50,20);
  if(s.indexDrawdownPct()<=-25){score-=20;reasons.add("지수가 고점 대비 25% 이상 하락했습니다.");} else if(s.indexDrawdownPct()<=-15){score-=12;reasons.add("강한 조정 구간입니다.");} else if(s.indexDrawdownPct()<=-8){score-=6;reasons.add("일반 조정 구간입니다.");} else if(s.indexDrawdownPct()>=15){score-=10;reasons.add("단기간 급등으로 과열 가능성이 있습니다.");}
  if(s.breadthPct()<30){score-=8;reasons.add("시장 내부 체력이 약합니다.");} else if(s.breadthPct()>65){score+=5;reasons.add("시장 확산도가 양호합니다.");}
  score=Math.max(0,Math.min(100,score)); MarketRegime regime;
  if(sentiment.structuralDamageRisk()||score<25) regime=MarketRegime.CRASH_RISK; else if(s.indexDrawdownPct()<=-15||score<40) regime=MarketRegime.STRONG_CORRECTION; else if(s.indexDrawdownPct()<=-8||score<55) regime=MarketRegime.MILD_CORRECTION; else if(s.indexDrawdownPct()>=15&&sentiment.sentimentRiskScore()<45) regime=MarketRegime.OVERHEATED; else regime=MarketRegime.NORMAL;
  reasons.add("주요 시장 내러티브: "+s.dominantNarrative()); return new MarketAssessment(regime,score,sentiment,List.copyOf(reasons));
 }
 private int norm(int v,int c,int m){double r=(v-c)/50.0;return (int)Math.round(Math.max(-m,Math.min(m,r*m)));}
}
