package com.nanum.investment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_brf")
@Getter
@Setter
@NoArgsConstructor
public class TbInvestmentBriefing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brf_id")
    private Long briefingId;

    @Column(name = "brf_dt", nullable = false, unique = true)
    private LocalDate briefingDate;

    @Column(name = "brf_ttl", nullable = false, length = 200)
    private String title;

    @Column(name = "brf_st", nullable = false, length = 20)
    private String briefingStatus = "DRAFT";
    @Column(name = "reg_dt", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "mod_dt", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }
}



