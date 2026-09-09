package com.sajitar.backend.adapter.out.persistence.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfileJpaEntity")
class ProfileJpaEntityTest {

    @Test
    @DisplayName("assignIdIfAbsent gera UUID quando o id é nulo e preserva id existente")
    void assignIdIfAbsent() {
        final var withoutId = new ProfileJpaEntity();
        withoutId.assignIdIfAbsent();
        assertThat(withoutId.getId()).isNotNull();

        final var existing = withoutId.getId();
        withoutId.assignIdIfAbsent();
        assertThat(withoutId.getId()).isEqualTo(existing);
    }

}
