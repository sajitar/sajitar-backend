package com.sajitar.backend.configuration;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@EnableConfigurationProperties(SchemaProperties.class)
class SchemaAfterConfiguration {

    static final String SCHEMA_AFTER = "schemaAfter";

    static final String ENTITY_MANAGER_FACTORY = "entityManagerFactory";

    private static final String SEPARATOR = ";";

    @Bean(name = SCHEMA_AFTER)
    @DependsOn(ENTITY_MANAGER_FACTORY)
    InitializingBean schemaAfter(final DataSource dataSource, final SchemaProperties properties) {
        return () -> DatabasePopulatorUtils.execute(populator(properties.afterScripts()), dataSource);
    }

    ResourceDatabasePopulator populator(final List<String> scripts) {
        final var populator = new ResourceDatabasePopulator();
        populator.setSeparator(SEPARATOR);
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.setContinueOnError(false);
        for (final var script : scripts) {
            populator.addScript(new ClassPathResource(script));
        }
        return populator;
    }

}
