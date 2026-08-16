package com.nanum.investment.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(
    name = "\"TB_IDX\"",
    uniqueConstraints = @UniqueConstraint(name = "UK_TB_IDX_01", columnNames = "IDX_CD"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TbIdx {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "IDX_ID")
  private Long indexId;

  @Column(name = "IDX_CD", nullable = false, length = 30)
  private String indexCode;

  @Column(name = "IDX_NM", nullable = false, length = 150)
  private String indexName;

  @Column(name = "IDX_NM_EN", length = 200)
  private String indexEnglishName;

  @Enumerated(EnumType.STRING)
  @Column(name = "IDX_TP", nullable = false, length = 30)
  private IndexType indexType;

  @Column(name = "MKT_CD", length = 30)
  private String marketCode;

  @Column(name = "CNTRY_CD", nullable = false, length = 10)
  private String countryCode;

  @Column(name = "CURR_CD", nullable = false, length = 10)
  private String currencyCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "DATA_SRC_CD", nullable = false, length = 30)
  private DataSourceCode dataSourceCode;

  @Column(name = "SRC_SYMBOL", length = 50)
  private String sourceSymbol;

  @Builder.Default
  @Column(name = "DFLT_YN", nullable = false, length = 1)
  private String defaultYn = "N";

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
