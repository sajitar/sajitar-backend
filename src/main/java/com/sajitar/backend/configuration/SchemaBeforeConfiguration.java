package com.sajitar.backend.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@EnableConfigurationProperties(SchemaProperties.class)
class SchemaBeforeConfiguration {

    @Bean
    static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnSchemaBefore() {
        return new EntityManagerFactoryDependsOnPostProcessor("schemaBefore");
    }

    @Bean
    InitializingBean schemaBefore(final DataSource dataSource, final SchemaProperties properties) {
        return () -> DatabasePopulatorUtils.execute(schemaBeforePopulator(properties), dataSource);
    }

    ResourceDatabasePopulator schemaBeforePopulator(final SchemaProperties properties) {
        final var populator = new ResourceDatabasePopulator();
        populator.setSeparator(";;");
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(false);
        for (final var location : properties.beforeScripts()) {
            populator.addScript(new ClassPathResource(location));
        }
        return populator;
    }

}
