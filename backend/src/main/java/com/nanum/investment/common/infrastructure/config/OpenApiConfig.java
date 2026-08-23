package com.nanum.investment.common.infrastructure.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI investmentOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Investment Briefing Engine Local API")
                .version("v1")
                .description(
                    "현재 프로젝트에서 제공하는 투자 브리핑·시장데이터·운영관리 Local REST API. "
                        + "Swagger UI의 Try it out으로 로컬 API를 직접 호출할 수 있습니다."))
        .addServersItem(new Server().url("http://localhost:8081").description("Docker Compose 로컬 백엔드"))
        .addServersItem(new Server().url("/").description("현재 접속 서버"));
  }
}
