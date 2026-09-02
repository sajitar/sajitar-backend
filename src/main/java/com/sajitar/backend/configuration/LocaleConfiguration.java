package com.sajitar.backend.configuration;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LocaleConfiguration {

    @Bean
    public LocaleResolver localeResolver() {
        return new QueryLangLocaleResolver();
    }

    @Bean
    public MessageSource messageSource() {
        final var source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(Locale.ENGLISH);
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Bean
    public LocalValidatorFactoryBean validator(final MessageSource messageSource) {
        final var factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource);
        return factory;
    }

}
