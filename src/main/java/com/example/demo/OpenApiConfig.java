package com.example.demo;

import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VPS Deploy API")
                        .version("1.0")
                        .description("API de test pour le pipeline CI/CD"));
    }

}
