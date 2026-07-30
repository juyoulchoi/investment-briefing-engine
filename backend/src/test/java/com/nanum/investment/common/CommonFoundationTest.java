package com.nanum.investment.common;

import com.nanum.investment.common.entity.SoftDeleteEntity;
import com.nanum.investment.common.response.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CommonFoundationTest {
 @Test void softDeleteAlsoDeactivates(){
  Sample entity=new Sample(); entity.softDelete();
  assertThat(entity.isDeleted()).isTrue(); assertThat(entity.getUseYn()).isEqualTo("N");
 }
 @Test void apiResponseKeepsTraceId(){
  var response=ApiResponse.success("ok","trace-1");
  assertThat(response.success()).isTrue(); assertThat(response.traceId()).isEqualTo("trace-1");
 }
 @Test void pageResponseCopiesMetadata(){
  var response=PageResponse.from(new PageImpl<>(List.of("a","b")));
  assertThat(response.content()).hasSize(2); assertThat(response.totalElements()).isEqualTo(2);
 }
 private static final class Sample extends SoftDeleteEntity {}
}
