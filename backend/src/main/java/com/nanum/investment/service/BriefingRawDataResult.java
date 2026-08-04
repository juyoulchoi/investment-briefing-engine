package com.nanum.investment.service;

import com.nanum.investment.domain.DataStatus;
import java.time.LocalDate;
import java.util.List;

public record BriefingRawDataResult(Long briefingId,LocalDate baseDate,Integer calculationSequence,
        Long investmentDecisionId,DataStatus dataStatus,int confidence,String sha256,List<String> sections) {}
