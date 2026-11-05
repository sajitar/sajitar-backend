package com.sajitar.backend.configuration;

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

    @Bean
    @DependsOn("entityManagerFactory")
    InitializingBean schemaAfter(final DataSource dataSource, final SchemaProperties properties) {
        return () -> DatabasePopulatorUtils.execute(schemaAfterPopulator(properties), dataSource);
    }

    ResourceDatabasePopulator schemaAfterPopulator(final SchemaProperties properties) {
        final var populator = new ResourceDatabasePopulator();
        populator.setSeparator(";");
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(false);
        for (final var location : properties.afterScripts()) {
            populator.addScript(new ClassPathResource(location));
        }
        return populator;
    }

}
