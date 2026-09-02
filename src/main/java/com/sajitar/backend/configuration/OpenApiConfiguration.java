package com.sajitar.backend.configuration;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI sajitarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sajitar API")
                        .description("Contrato HTTP dos recursos expostos pelo backend Sajitar.")
                        .version("0.0.0"));
    }

    @Bean
    OpenApiCustomizer langQueryParameter() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            final var lang = new Parameter()
                    .in("query")
                    .name(QueryLangLocaleResolver.LANG_PARAMETER)
                    .required(false)
                    .description(
                            "Response language: en (default), pt or es. Omitted, blank or unsupported values use English.")
                    .schema(new StringSchema()._enum(List.of("en", "pt", "es")));
            openApi.getPaths().values().forEach(item -> item.readOperations().forEach(op -> op.addParametersItem(lang)));
        };
    }

}
