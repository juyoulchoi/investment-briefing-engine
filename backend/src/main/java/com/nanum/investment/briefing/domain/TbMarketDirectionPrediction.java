package com.nanum.investment.briefing.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "\"TB_MKT_DIR_PRED\"")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TbMarketDirectionPrediction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "MKT_DIR_PRED_ID") private Long id;
  @Column(name = "BASE_DT", nullable = false) private LocalDate baseDate;
  @Column(name = "CALC_SEQ", nullable = false) private Integer calculationSequence;
  @Column(name = "DIR_SCR", nullable = false) private Integer directionScore;
  @Column(name = "UPTREND_RESUME_PROB", nullable = false) private Integer uptrendResumeProbability;
  @Column(name = "BOX_RANGE_PROB", nullable = false) private Integer boxRangeProbability;
  @Column(name = "RE_CORRECTION_PROB", nullable = false) private Integer reCorrectionProbability;
  @Column(name = "RETEST_LOW_PROB", nullable = false) private Integer retestLowProbability;
  @Column(name = "UPTREND_RESUME_CHG", nullable = false) private Integer uptrendResumeChange;
  @Column(name = "BOX_RANGE_CHG", nullable = false) private Integer boxRangeChange;
  @Column(name = "RE_CORRECTION_CHG", nullable = false) private Integer reCorrectionChange;
  @Column(name = "RETEST_LOW_CHG", nullable = false) private Integer retestLowChange;
  @Column(name = "INPUT_BASE_DT_JSON", nullable = false, columnDefinition = "jsonb") private String inputBaseDateJson;
  @Column(name = "CALC_BASIS_JSON", nullable = false, columnDefinition = "jsonb") private String calculationBasisJson;
  @Builder.Default @Column(name = "RULE_VER_NO", nullable = false) private Integer ruleVersion = 1;
  @Builder.Default @Column(name = "LATEST_YN", nullable = false) private String latestYn = "Y";
  @Column(name = "CRT_DTTM", insertable = false, updatable = false) private OffsetDateTime createdAt;
}
