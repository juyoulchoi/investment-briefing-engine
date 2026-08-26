package com.nanum.investment.common.domain;

import com.nanum.investment.marketdata.domain.TbIdx;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(
    name = "\"TB_STK\"",
    uniqueConstraints = {@UniqueConstraint(name = "UK_TB_STK_01", columnNames = "STK_CD")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbStk {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "STK_ID")
  private Long stockId;

  @Column(name = "STK_CD", nullable = false, length = 30)
  private String stockCode;

  @Column(name = "STK_NM", nullable = false, length = 150)
  private String stockName;

  @Column(name = "STK_NM_EN", length = 200)
  private String stockEnglishName;

  @Column(name = "MKT_CD", nullable = false, length = 30)
  private String marketCode;

  @Column(name = "CNTRY_CD", nullable = false, length = 10)
  private String countryCode;

  @Column(name = "CURR_CD", nullable = false, length = 10)
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "AST_TP", nullable = false, length = 30)
  private AssetType assetType;

  @Enumerated(EnumType.STRING)
  @Column(name = "STK_GRADE", nullable = false, length = 20)
  private StockGrade stockGrade;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "BASE_IDX_CD", foreignKey = @ForeignKey(name = "FK_TB_STK_01"))
  private TbIdx baseIndex;

  @Column(name = "SECT_CD", length = 30)
  private String sectorCode;

  @Column(name = "SECT_NM", length = 100)
  private String sectorName;

  @Column(name = "INDST_CD", length = 30)
  private String industryCode;

  @Column(name = "INDST_NM", length = 100)
  private String industryName;

  @Builder.Default
  @Column(name = "REG_BUY_YN", nullable = false, length = 1)
  private String regularBuyYn = "Y";

  @Builder.Default
  @Column(name = "ADD_BUY_YN", nullable = false, length = 1)
  private String additionalBuyYn = "Y";

  @Builder.Default
  @Column(name = "REBUY_YN", nullable = false, length = 1)
  private String rebuyYn = "Y";

  @Builder.Default
  @Column(name = "FUND_DMG_YN", nullable = false, length = 1)
  private String fundamentalDamageYn = "N";

  @Builder.Default
  @Column(name = "THEME_RISK_YN", nullable = false, length = 1)
  private String themeRiskYn = "N";

  @Builder.Default
  @Column(name = "USE_YN", nullable = false, length = 1)
  private String useYn = "Y";

  @Builder.Default
  @Column(name = "DEL_YN", nullable = false, length = 1)
  private String deleteYn = "N";

  @Column(name = "CRT_DTTM", nullable = false, insertable = false, updatable = false)
  private OffsetDateTime createdDateTime;

  @Column(name = "CRT_USR_ID", length = 50)
  private String createdUserId;

  @Column(name = "UPD_DTTM", nullable = false, insertable = false)
  private OffsetDateTime updatedDateTime;

  @Column(name = "UPD_USR_ID", length = 50)
  private String updatedUserId;
}
