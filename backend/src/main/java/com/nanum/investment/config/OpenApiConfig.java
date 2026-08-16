package com.nanum.investment.config;

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
                .title("Investment Briefing API")
                .version("v1")
                .description("투자 브리핑 계산·시장데이터·운영관리 REST API"))
        .addServersItem(new Server().url("/").description("현재 서버"));
  }
}
