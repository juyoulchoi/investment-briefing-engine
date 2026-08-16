package com.nanum.investment.briefing.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "\"TB_SCH_LOG\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbSchLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "SCH_LOG_ID")
  private Long schedulerLogId;

  @Column(name = "TRACE_ID", nullable = false, length = 100)
  private String traceId;

  @Column(name = "JOB_CD", nullable = false, length = 50)
  private String jobCode;

  @Column(name = "JOB_NM", nullable = false, length = 150)
  private String jobName;

  @Column(name = "BASE_DT")
  private LocalDate baseDate;

  @Column(name = "START_DTTM", nullable = false)
  private OffsetDateTime startDateTime;

  @Column(name = "END_DTTM")
  private OffsetDateTime endDateTime;

  @Enumerated(EnumType.STRING)
  @Column(name = "JOB_STS", nullable = false, length = 20)
  private SchedulerJobStatus jobStatus;

  @Builder.Default
  @Column(name = "SUCC_CNT", nullable = false)
  private Integer successCount = 0;

  @Builder.Default
  @Column(name = "FAIL_CNT", nullable = false)
  private Integer failureCount = 0;

  @Builder.Default
  @Column(name = "SKIP_CNT", nullable = false)
  private Integer skippedCount = 0;

  @Column(name = "ELAPSED_MS")
  private Long elapsedMilliseconds;

  @Column(name = "ERR_MSG", length = 2000)
  private String errorMessage;
}
