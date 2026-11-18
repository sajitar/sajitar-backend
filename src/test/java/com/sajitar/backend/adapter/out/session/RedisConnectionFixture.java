package com.sajitar.backend.adapter.out.session;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import lombok.experimental.UtilityClass;

/**
 * Redis de {@code spring.data.redis.*} no YAML do perfil ativo: o host muda entre
 * {@code LOCAL-TEST}, {@code CI} e {@code DOCKER-TEST}.
 */
@UtilityClass
class RedisConnectionFixture {

    private static final Environment ENVIRONMENT = profileEnvironment();

    static final String HOST = ENVIRONMENT.getRequiredProperty("spring.data.redis.host");

    static final int PORT = ENVIRONMENT.getRequiredProperty("spring.data.redis.port", Integer.class);

    static LettuceConnectionFactory connectionFactory(final int port) {
        final var configuration = new RedisStandaloneConfiguration(HOST, port);
        configuration.setUsername(ENVIRONMENT.getRequiredProperty("spring.data.redis.username"));
        configuration.setPassword(ENVIRONMENT.getRequiredProperty("spring.data.redis.password"));
        final var factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        return factory;
    }

    private static Environment profileEnvironment() {
        final var environment = new StandardEnvironment();
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

}
