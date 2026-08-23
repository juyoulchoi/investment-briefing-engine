package com.nanum.investment.common.api;

import com.nanum.investment.common.application.CommonCodeLookupService;
import com.nanum.investment.common.application.CommonCodeLookupService.CommonCode;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/common-codes")
@io.swagger.v3.oas.annotations.tags.Tag(name = "기준정보", description = "공통코드 및 기준정보 API")
public class CommonCodeController {
  private final CommonCodeLookupService commonCodes;

  public CommonCodeController(CommonCodeLookupService commonCodes) {
    this.commonCodes = commonCodes;
  }

  @GetMapping("/{group}")
  @io.swagger.v3.oas.annotations.Operation(summary = "공통코드 그룹 조회")
  public List<CommonCode> findActiveByGroup(@PathVariable String group) {
    return commonCodes.activeCodes(group);
  }
}
