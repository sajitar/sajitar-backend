package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@DisplayName("SchemaDdlConfiguration")
class SchemaDdlConfigurationTest {

    private final SchemaDdlConfiguration configuration = new SchemaDdlConfiguration();

    @Test
    @DisplayName("Registra SchemaProperties no contexto")
    void enablesSchemaProperties() {
        assertThat(SchemaDdlConfiguration.class.getAnnotation(EnableConfigurationProperties.class).value())
                .containsExactly(SchemaProperties.class);
    }

    @Test
    @DisplayName("Customizer grava hibernate.hbm2ddl.auto")
    void customizerWritesDdlAuto() {
        final var properties = SchemaPropertiesFixture.local();
        final var hibernateProperties = new HashMap<String, Object>();

        configuration.schemaDdlAuto(properties).customize(hibernateProperties);

        assertThat(hibernateProperties).containsEntry(SchemaDdlConfiguration.HBM2DDL_AUTO, properties.ddlAuto());
    }

}
