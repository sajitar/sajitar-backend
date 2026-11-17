package com.sajitar.backend.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SchemaProperties.class)
class SchemaDdlConfiguration {

    static final String HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

    @Bean
    HibernatePropertiesCustomizer schemaDdlAuto(final SchemaProperties properties) {
        return hibernateProperties -> hibernateProperties.put(HBM2DDL_AUTO, properties.ddlAuto());
    }

}
