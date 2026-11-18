package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("Perfis da aplicação")
class ApplicationProfilesTest {

    @Test
    @DisplayName("O application.yml só declara o nome da aplicação e o perfil ativo")
    void mainFileDeclaresOnlyNameAndActiveProfile() {
        final var properties = properties("application.yml");

        assertThat(properties.stringPropertyNames())
                .containsExactlyInAnyOrder("spring.application.name", "spring.profiles.active");
        assertThat(properties.getProperty("spring.profiles.active")).isEqualTo("${SPRING_PROFILES_ACTIVE}");
    }

    @ParameterizedTest
    @ValueSource(strings = { "LOCAL", "LOCAL-TEST", "CI", "DEMO", "DOCKER", "DOCKER-TEST" })
    @DisplayName("O YAML do perfil não lê variável de ambiente")
    void profileFileUsesLiteralValues(final String profile) {
        assertThat(properties("application-" + profile + ".yml").values())
                .isNotEmpty()
                .map(String::valueOf)
                .noneMatch(value -> value.contains("${"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "LOCAL", "LOCAL-TEST", "CI", "DEMO", "DOCKER", "DOCKER-TEST" })
    @DisplayName("Cada perfil liga as propriedades do Sajitar, o banco e o Redis")
    void bindsSajitarProperties(final String profile) {
        final var environment = environment(profile);
        final var binder = Binder.get(environment);

        assertThat(binder.bind("sajitar.schema", SchemaProperties.class).get().beforeScripts())
                .containsExactly("util/functions.sql", "util/enums.sql");
        assertThat(binder.bind("sajitar.security.jwt", JwtProperties.class).get().issuer())
                .isEqualTo("sajitar-backend");
        assertThat(binder.bind("sajitar.security.attempt", AttemptProperties.class).get().trustForwardedFor())
                .isFalse();
        assertThat(binder.bind("sajitar.profile", ProfilePurgeProperties.class).get().signInMaxAgeMinutes())
                .isEqualTo(30);
        assertThat(binder.bind("sajitar.domain.validation", ValidationProperties.class).get().limit().max())
                .isEqualTo(100);
        assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:postgresql://");
        assertThat(environment.getProperty("spring.data.redis.host")).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = { "LOCAL", "DEMO", "DOCKER" })
    @DisplayName("Os perfis da API ligam o SMTP do Mailpit")
    void bindsMailProperties(final String profile) {
        final var mail = Binder.get(environment(profile)).bind("sajitar.mail", MailProperties.class).get();

        assertThat(mail.port()).isEqualTo(1025);
        assertThat(mail.from()).isEqualTo("noreply@localhost");
    }

    private static Properties properties(final String location) {
        final var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(location));
        return yaml.getObject();
    }

    private static ConfigurableEnvironment environment(final String profile) {
        final var environment = new MockEnvironment().withProperty("spring.profiles.active", profile);
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

}
