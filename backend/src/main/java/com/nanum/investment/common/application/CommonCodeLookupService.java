package com.nanum.investment.common.application;

import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class CommonCodeLookupService {
  private final JdbcClient jdbc;

  public CommonCodeLookupService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, String> activeNames(String group) {
    LinkedHashMap<String, String> names = new LinkedHashMap<>();
    activeCodes(group).forEach(code -> names.put(code.code(), code.name()));
    return Collections.unmodifiableMap(names);
  }

  public List<CommonCode> activeCodes(String group) {
    if (group == null || group.isBlank())
      throw new IllegalArgumentException("공통코드 그룹이 필요합니다.");
    return jdbc
        .sql(
            """
            SELECT "CD_KEY", "CD_NM", "DESC", "DSP_ORD"
              FROM "TB_CD_DTL"
             WHERE "CD_GRP" = :group
               AND "ACTV_YN" = 'Y'
             ORDER BY "DSP_ORD", "CD_KEY"
            """)
        .param("group", group)
        .query(
            (rs, rowNum) ->
                new CommonCode(
                    rs.getString("CD_KEY"),
                    rs.getString("CD_NM"),
                    rs.getString("DESC"),
                    rs.getInt("DSP_ORD")))
        .list();
  }

  public record CommonCode(String code, String name, String description, int displayOrder) {}
}
