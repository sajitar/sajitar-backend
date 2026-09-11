package com.sajitar.backend.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DisplayName("BCryptPasswordHasher")
class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());

    @Test
    @DisplayName("matches é verdadeiro quando a senha coincide com o hash")
    void matchesWhenPasswordIsCorrect() {
        final var hash = hasher.hash("senhaSegura1");

        assertThat(hasher.matches("senhaSegura1", hash)).isTrue();
    }

    @Test
    @DisplayName("matches é falso quando a senha não coincide com o hash")
    void doesNotMatchWhenPasswordIsWrong() {
        final var hash = hasher.hash("senhaSegura1");

        assertThat(hasher.matches("outraSenha1", hash)).isFalse();
    }

}
