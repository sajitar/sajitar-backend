package com.sajitar.backend.domain.port.authority;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.authority.Authority;

@DisplayName("AuthorityPageCriteria")
class AuthorityPageCriteriaTest {

    @Test
    @DisplayName("hasCursor é verdadeiro somente quando lastSeenType está preenchido")
    void hasCursorWhenLastSeenTypeIsPresent() {
        final var profileId = UUID.randomUUID();
        assertThat(new AuthorityPageCriteria(profileId, null, 10, false).hasCursor()).isFalse();
        assertThat(new AuthorityPageCriteria(profileId, Authority.Type.MASTER, 10, false).hasCursor()).isTrue();
    }

    @Test
    @DisplayName("withCursor preserva profileId e limit e troca tipo e reverse")
    void withCursorReplacesTypeAndReverse() {
        final var profileId = UUID.randomUUID();
        final var original = new AuthorityPageCriteria(profileId, Authority.Type.MASTER, 5, false);
        final var next = original.withCursor(Authority.Type.MEMBER, true);
        assertThat(next.profileId()).isEqualTo(profileId);
        assertThat(next.limit()).isEqualTo(5);
        assertThat(next.lastSeenType()).isEqualTo(Authority.Type.MEMBER);
        assertThat(next.reverse()).isTrue();
    }

}
