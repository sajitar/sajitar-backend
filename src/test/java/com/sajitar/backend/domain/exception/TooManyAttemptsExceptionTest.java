package com.sajitar.backend.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("TooManyAttemptsException")
class TooManyAttemptsExceptionTest {

    @Test
    @DisplayName("Credenciais usam o campo credentials")
    void credentialsField() {
        final var exception = TooManyAttemptsException.forCredentials(Duration.ofSeconds(5));

        assertThat(exception.content()).containsOnlyKeys("credentials");
        assertThat(exception.content().get("credentials")).containsExactly(TooManyAttemptsException.MESSAGE_KEY);
    }

    @Test
    @DisplayName("Refresh usa o campo refreshToken")
    void refreshTokenField() {
        final var exception = TooManyAttemptsException.forRefreshToken(Duration.ofSeconds(5));

        assertThat(exception.content()).containsOnlyKeys("refreshToken");
        assertThat(exception.content().get("refreshToken")).containsExactly(TooManyAttemptsException.MESSAGE_KEY);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1",
            "1, 1",
            "1000, 1",
            "1001, 2"
    })
    @DisplayName("Retry-After arredonda milissegundos para cima com mínimo 1")
    void roundsRetryAfterUpToSeconds(final long millis, final long seconds) {
        assertThat(TooManyAttemptsException.forCredentials(Duration.ofMillis(millis)).retryAfterSeconds())
                .isEqualTo(seconds);
    }

}
