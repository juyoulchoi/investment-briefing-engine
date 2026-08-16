package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_inv_exec")
@Getter
@Setter
@NoArgsConstructor
public class TbInvestmentExecution {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "execution_id")
  private Long executionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "briefing_id")
  private TbInvestmentBriefing briefing;

  @Column(name = "execution_date", nullable = false)
  private LocalDate executionDate;

  @Column(name = "account_type_group", nullable = false, length = 50)
  private String accountTypeGroup = "ACCOUNT_TYPE";

  @Column(name = "account_type", nullable = false, length = 30)
  private String accountType;

  @Column(name = "stock_code", nullable = false, length = 30)
  private String stockCode;

  @Column(name = "stock_name", nullable = false, length = 100)
  private String stockName;

  @Column(name = "action_type", nullable = false, length = 20)
  private String actionType;

  @Column(precision = 18, scale = 6)
  private BigDecimal quantity;

  @Column(precision = 18, scale = 4)
  private BigDecimal price;

  @Column(precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "execution_status", length = 20)
  private String executionStatus;

  @Column(name = "source_type_group", nullable = false, length = 50)
  private String sourceTypeGroup = "EXECUTION_SOURCE_TYPE";

  @Column(name = "source_type", length = 20)
  private String sourceType;

  @Column(columnDefinition = "TEXT")
  private String memo;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;
}
