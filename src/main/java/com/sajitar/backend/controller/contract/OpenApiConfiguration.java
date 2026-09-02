package com.sajitar.backend.controller.contract;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI sajitarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sajitar API")
                        .description("Contrato HTTP dos recursos expostos pelo backend Sajitar.")
                        .version("0.0.0"));
    }

}
