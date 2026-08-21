package com.nanum.investment.common.application;

import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class CommonCodeLookupService {
  private final JdbcClient jdbc;

  public CommonCodeLookupService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, String> activeNames(String group) {
    return jdbc
        .sql(
            """
            SELECT "CD_KEY", "CD_NM"
              FROM "TB_CD_DTL"
             WHERE "CD_GRP" = :group
               AND "ACTV_YN" = 'Y'
             ORDER BY "DSP_ORD", "CD_KEY"
            """)
        .param("group", group)
        .query((rs, rowNum) -> Map.entry(rs.getString("CD_KEY"), rs.getString("CD_NM")))
        .list()
        .stream()
        .collect(
            java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
