package com.nanum.investment.common.api;

import com.nanum.investment.common.application.CommonCodeLookupService;
import com.nanum.investment.common.application.CommonCodeLookupService.CommonCode;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/common-codes")
public class CommonCodeController {
  private final CommonCodeLookupService commonCodes;

  public CommonCodeController(CommonCodeLookupService commonCodes) {
    this.commonCodes = commonCodes;
  }

  @GetMapping("/{group}")
  public List<CommonCode> findActiveByGroup(@PathVariable String group) {
    return commonCodes.activeCodes(group);
  }
}
