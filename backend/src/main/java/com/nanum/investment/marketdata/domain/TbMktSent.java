package com.nanum.investment.marketdata.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(
    name = "\"TB_MKT_SENT\"",
    uniqueConstraints =
        @UniqueConstraint(
            name = "UK_TB_MKT_SENT_01",
            columnNames = {"BASE_DT", "MKT_SNAP_CD"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbMktSent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "MKT_SENT_ID")
  private Long marketSentimentId;

  @Column(name = "BASE_DT", nullable = false)
  private LocalDate baseDate;

  @Column(name = "MKT_SNAP_CD", nullable = false, length = 30)
  private String marketSnapshotCode;

  @Column(name = "NEWS_FEAR_SCR", precision = 10, scale = 4)
  private BigDecimal newsFearScore;

  @Column(name = "AI_FATIGUE_SCR", precision = 10, scale = 4)
  private BigDecimal aiFatigueScore;

  @Column(name = "EARN_CONF_SCR", precision = 10, scale = 4)
  private BigDecimal earningsConfidenceScore;

  @Column(name = "SENT_SCR", nullable = false, precision = 10, scale = 4)
  private BigDecimal sentimentScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "SENT_PHASE", nullable = false, length = 20)
  private SentimentPhase sentimentPhase;

  @Column(name = "CONF_RT", nullable = false, precision = 10, scale = 4)
  private BigDecimal confidenceRate;

  @Builder.Default
  @Column(name = "STRUCT_DMG_YN", nullable = false, length = 1)
  private String structuralDamageYn = "N";

  @Builder.Default
  @Column(name = "RULE_VER_NO", nullable = false)
  private Integer ruleVersionNumber = 1;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "DATA_STS", nullable = false, length = 20)
  private DataStatus dataStatus = DataStatus.FRESH;
}
