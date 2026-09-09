package com.sajitar.backend.adapter.out.persistence.authority;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AuthorityJpaEntity")
class AuthorityJpaEntityTest {

    @Test
    @DisplayName("assignIdIfAbsent gera UUID quando o id é nulo e preserva id existente")
    void assignIdIfAbsent() {
        final var withoutId = new AuthorityJpaEntity();
        withoutId.assignIdIfAbsent();
        assertThat(withoutId.getId()).isNotNull();

        final var existing = withoutId.getId();
        withoutId.assignIdIfAbsent();
        assertThat(withoutId.getId()).isEqualTo(existing);
    }

}
