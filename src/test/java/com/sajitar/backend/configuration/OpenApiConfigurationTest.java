package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

@DisplayName("OpenApiConfiguration")
class OpenApiConfigurationTest {

    private final OpenApiConfiguration configuration = new OpenApiConfiguration();

    @Test
    @DisplayName("Documento OpenAPI traz título e versão do produto")
    void sajitarOpenApiHasProductInfo() {
        final var openApi = configuration.sajitarOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Sajitar API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("0.0.0");
    }

    @Test
    @DisplayName("Customizer não altera OpenAPI sem paths")
    void langCustomizerSkipsWhenPathsAreNull() {
        final var openApi = new OpenAPI();

        configuration.langQueryParameter().customise(openApi);

        assertThat(openApi.getPaths()).isNull();
    }

    @Test
    @DisplayName("Customizer adiciona query lang em todas as operações")
    void langCustomizerAddsQueryParameter() {
        final var get = new Operation().operationId("listProfiles");
        final var post = new Operation().operationId("createProfile");
        final var item = new PathItem().get(get).post(post);
        final var openApi = configuration.sajitarOpenApi()
                .paths(new Paths().addPathItem("/profiles", item));

        configuration.langQueryParameter().customise(openApi);

        assertThat(get.getParameters()).anySatisfy(parameter -> {
            assertThat(parameter.getName()).isEqualTo(QueryLangLocaleResolver.LANG_PARAMETER);
            assertThat(parameter.getIn()).isEqualTo("query");
            assertThat(parameter.getRequired()).isFalse();
        });
        assertThat(post.getParameters()).anySatisfy(parameter -> {
            assertThat(parameter.getName()).isEqualTo(QueryLangLocaleResolver.LANG_PARAMETER);
        });
    }

}
