package io.github.spojchil.infopilot.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InfoPilot API")
                        .description("企业文档智能助手 — 对话、RAG、Agent 接口")
                        .version("1.0"));
    }
}
