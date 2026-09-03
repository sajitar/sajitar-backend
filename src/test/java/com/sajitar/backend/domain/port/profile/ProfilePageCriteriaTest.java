package com.sajitar.backend.domain.port.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfilePageCriteria")
class ProfilePageCriteriaTest {

    @Test
    @DisplayName("hasNameFilter e hasCursor cobrem nulo, em branco e preenchido")
    void nameFilterAndCursorFlags() {
        final var id = UUID.randomUUID();
        assertThat(new ProfilePageCriteria(null, null, null, 10, false).hasNameFilter()).isFalse();
        assertThat(new ProfilePageCriteria("   ", null, null, 10, false).hasNameFilter()).isFalse();
        assertThat(new ProfilePageCriteria("Silva", null, null, 10, false).hasNameFilter()).isTrue();

        assertThat(new ProfilePageCriteria(null, null, id, 10, false).hasCursor()).isFalse();
        assertThat(new ProfilePageCriteria(null, "   ", id, 10, false).hasCursor()).isFalse();
        assertThat(new ProfilePageCriteria(null, "Maria Silva", null, 10, false).hasCursor()).isFalse();
        assertThat(new ProfilePageCriteria(null, "Maria Silva", id, 10, false).hasCursor()).isTrue();
    }

}
