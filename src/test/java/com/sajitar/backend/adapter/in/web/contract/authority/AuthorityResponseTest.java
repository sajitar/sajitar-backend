package com.sajitar.backend.adapter.in.web.contract.authority;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.authority.Authority;

@DisplayName("AuthorityResponse")
class AuthorityResponseTest {

    @Test
    @DisplayName("from copia id, profileId e type")
    void fromCopiesAllAttributes() {
        final var authority = new Authority(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Authority.Type.MASTER);

        final var response = AuthorityResponse.from(authority);

        assertThat(response.id()).isEqualTo(authority.id());
        assertThat(response.profileId()).isEqualTo(authority.profileId());
        assertThat(response.type()).isEqualTo(Authority.Type.MASTER);
    }

    @Test
    @DisplayName("página copia content e metadados de Page")
    void pageFromCopiesPage() {
        final var authority = Authority.create(
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Authority.Type.MEMBER);
        final var response = AuthorityPageResponse.from(new Page<>(List.of(authority), 1, 2, false));
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(authority.id());
        assertThat(response.content().getFirst().type()).isEqualTo(Authority.Type.MEMBER);
        assertThat(response.precedingElements()).isEqualTo(1);
        assertThat(response.followingElements()).isEqualTo(2);
        assertThat(response.reverse()).isFalse();
    }

}
