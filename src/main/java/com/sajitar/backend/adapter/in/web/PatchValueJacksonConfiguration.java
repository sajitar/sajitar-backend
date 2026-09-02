package com.sajitar.backend.adapter.in.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sajitar.backend.application.command.PatchValue;

import tools.jackson.databind.module.SimpleModule;

@Configuration
class PatchValueJacksonConfiguration {

    @Bean
    JsonMapperBuilderCustomizer patchValueDeserializer() {
        return builder -> builder.addModule(patchValueModule());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static SimpleModule patchValueModule() {
        final var module = new SimpleModule("PatchValueModule");
        module.addDeserializer(PatchValue.class, new PatchValueDeserializer());
        return module;
    }

}
