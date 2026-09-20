package com.sajitar.backend.adapter.in.web.contract.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.profile.Profile;

@DisplayName("ProfileResponse")
class ProfileResponseTest {

    @Test
    @DisplayName("summary from copia id, name e description")
    void summaryFromCopiesAttributes() {
        final var profile = new Profile(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                "user@example.com",
                "hashed-password");

        final var response = ProfileSummaryResponse.from(profile);

        assertThat(response.id()).isEqualTo(profile.id());
        assertThat(response.name()).isEqualTo(profile.name());
        assertThat(response.description()).isEqualTo(profile.description());
    }

    @Test
    @DisplayName("details from copia campos de leitura sem senha")
    void detailsFromCopiesReadableAttributesWithoutPassword() {
        final var profile = new Profile(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                "user@example.com",
                "hashed-password");

        final var response = ProfileDetailsResponse.from(profile);

        assertThat(response.id()).isEqualTo(profile.id());
        assertThat(response.name()).isEqualTo(profile.name());
        assertThat(response.description()).isEqualTo(profile.description());
        assertThat(response.birthday()).isEqualTo(profile.birthday());
        assertThat(response.email()).isEqualTo(profile.email());
    }

    @Test
    @DisplayName("página copia content e metadados de Page")
    void pageFromCopiesPage() {
        final var profile = new Profile(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                "user@example.com",
                "hashed-password");
        final var response = ProfilePageResponse.from(new Page<>(List.of(profile), 1, 2, false));
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(profile.id());
        assertThat(response.content().getFirst().name()).isEqualTo(profile.name());
        assertThat(response.content().getFirst().description()).isEqualTo(profile.description());
        assertThat(response.precedingElements()).isEqualTo(1);
        assertThat(response.followingElements()).isEqualTo(2);
        assertThat(response.reverse()).isFalse();
    }

}
