package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_reg_buy")
@Getter
@Setter
@NoArgsConstructor
public class TbRegularInvestmentSetting {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "setting_id")
  private Long settingId;

  @Column(name = "account_type_group", nullable = false, length = 50)
  private String accountTypeGroup = "ACCOUNT_TYPE";

  @Column(name = "account_type", nullable = false, length = 30)
  private String accountType;

  @Column(name = "stock_code", nullable = false, length = 30)
  private String stockCode;

  @Column(name = "stock_name", nullable = false, length = 100)
  private String stockName;

  @Column(name = "cycle_type_group", nullable = false, length = 50)
  private String cycleTypeGroup = "CYCLE_TYPE";

  @Column(name = "cycle_type", nullable = false, length = 20)
  private String cycleType;

  @Column(name = "day_of_week", length = 20)
  private String dayOfWeek;

  @Column(name = "day_of_month")
  private Integer dayOfMonth;

  @Column(precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(precision = 18, scale = 6)
  private BigDecimal quantity;

  @Column(name = "active_yn", nullable = false, length = 1)
  private String activeYn = "Y";

  @Column(name = "pause_reason", columnDefinition = "TEXT")
  private String pauseReason;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false)
  private LocalDateTime updatedAt;

  @PreUpdate
  void touchUpdatedAt() {
    updatedAt = LocalDateTime.now();
  }
}
