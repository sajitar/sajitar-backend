package com.sajitar.backend.adapter.out.persistence.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfilePageCriteria;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfilePersistenceAdapter")
class ProfilePersistenceAdapterTest {

    private static final UUID VIEWER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");

    private static final String READER = Profile.Type.READER.name();

    @Mock
    private ProfileJpaRepository jpa;

    private ProfilePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProfilePersistenceAdapter(jpa);
    }

    @Test
    @DisplayName("save, find e delete delegam ao JPA")
    void delegatesCrud() {
        final var domain = new Profile(
                UUID.fromString("019c0000-a111-7000-8000-111111111111"),
                Profile.Type.READER,
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                "user@example.com",
                "hashed-password",
                false);
        final var entity = ProfilePersistenceMapper.toEntity(domain);
        when(jpa.save(any(ProfileJpaEntity.class))).thenReturn(entity);
        when(jpa.findById(domain.id())).thenReturn(Optional.of(entity));
        when(jpa.findByEmail(domain.email())).thenReturn(Optional.of(entity));

        assertThat(adapter.save(domain).id()).isEqualTo(domain.id());
        assertThat(adapter.findById(domain.id())).contains(domain);
        assertThat(adapter.findByEmail(domain.email())).contains(domain);
        adapter.deleteById(domain.id());
        verify(jpa).deleteById(domain.id());
    }

    @Test
    @DisplayName("findUnverifiedCreatedBefore consulta VERIFY_EMAIL abaixo do UUIDv7 limite")
    void findUnverifiedCreatedBeforeUsesCutoffUuid() {
        final var cutoff = Instant.parse("2026-09-19T03:00:00Z");
        final var cutoffId = ProfilePersistenceAdapter.uuidV7At(cutoff);
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        when(jpa.findUnverifiedCreatedBefore(Checker.Type.VERIFY_EMAIL.name(), cutoffId))
                .thenReturn(List.of(profileId));

        assertThat(adapter.findUnverifiedCreatedBefore(cutoff)).containsExactly(profileId);
        verify(jpa).findUnverifiedCreatedBefore(Checker.Type.VERIFY_EMAIL.name(), cutoffId);
    }

    @Test
    @DisplayName("uuidV7At coloca o instante nos 48 bits e marca versão 7")
    void uuidV7AtEncodesTimestampAndVersion() {
        final var instant = Instant.parse("2026-09-19T03:00:00Z");
        final var id = ProfilePersistenceAdapter.uuidV7At(instant);

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
        assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(instant.toEpochMilli());
        assertThat(id.getLeastSignificantBits()).isEqualTo(0x8000000000000000L);
    }

    @Test
    @DisplayName("findPage sem filtro de nome: ASC e DESC com e sem cursor")
    void findPageWithoutNameFilter() {
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = Checker.Type.VERIFY_EMAIL.name();
        when(jpa.findAllAscending(10, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(List.of());
        when(jpa.findAllAscendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false))
                .thenReturn(List.of());
        when(jpa.findAllDescending(10, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(List.of());
        when(jpa.findAllDescendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false))
                .thenReturn(List.of());

        assertThat(adapter.findPage(page(null, null, null, 10, false, true))).isEmpty();
        assertThat(adapter.findPage(page(null, lastSeenName, lastSeenId, 2, false, true))).isEmpty();
        assertThat(adapter.findPage(page(null, null, null, 10, true, true))).isEmpty();
        assertThat(adapter.findPage(page(null, lastSeenName, lastSeenId, 2, true, true))).isEmpty();
        verify(jpa).findAllAscending(10, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).findAllAscendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).findAllDescending(10, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).findAllDescendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
    }

    @Test
    @DisplayName("findPage com filtro de nome: ASC e DESC com e sem cursor")
    void findPageWithNameFilter() {
        final var name = "Silva";
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = Checker.Type.VERIFY_EMAIL.name();
        when(jpa.findByNameContainingIgnoreCaseAscending(10, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false))
                .thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseAscendingAfter(
                2, lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false))
                .thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseDescending(10, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false))
                .thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseDescendingAfter(
                2, lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false))
                .thenReturn(List.of());

        assertThat(adapter.findPage(page(name, null, null, 10, false, true))).isEmpty();
        assertThat(adapter.findPage(page(name, lastSeenName, lastSeenId, 2, false, true))).isEmpty();
        assertThat(adapter.findPage(page(name, null, null, 10, true, true))).isEmpty();
        assertThat(adapter.findPage(page(name, lastSeenName, lastSeenId, 2, true, true))).isEmpty();
        verify(jpa).findByNameContainingIgnoreCaseAscending(10, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).findByNameContainingIgnoreCaseAscendingAfter(
                2, lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).findByNameContainingIgnoreCaseDescending(10, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).findByNameContainingIgnoreCaseDescendingAfter(
                2, lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
    }

    @Test
    @DisplayName("countAfterCursor com e sem filtro de nome em ASC e DESC")
    void countAfterCursorDirections() {
        final var name = "Silva";
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = Checker.Type.VERIFY_EMAIL.name();
        when(jpa.countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(2L);
        when(jpa.countForFindAllDescendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(1L);
        when(jpa.countForFindByNameContainingIgnoreCaseAscendingAfter(
                lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(3L);
        when(jpa.countForFindByNameContainingIgnoreCaseDescendingAfter(
                lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(4L);

        assertThat(adapter.countAfterCursor(page(null, lastSeenName, lastSeenId, 10, false, true))).isEqualTo(2L);
        assertThat(adapter.countAfterCursor(page(null, lastSeenName, lastSeenId, 10, true, true))).isEqualTo(1L);
        assertThat(adapter.countAfterCursor(page(name, lastSeenName, lastSeenId, 10, false, true))).isEqualTo(3L);
        assertThat(adapter.countAfterCursor(page(name, lastSeenName, lastSeenId, 10, true, true))).isEqualTo(4L);
        verify(jpa).countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).countForFindAllDescendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).countForFindByNameContainingIgnoreCaseAscendingAfter(
                lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).countForFindByNameContainingIgnoreCaseDescendingAfter(
                lastSeenName, lastSeenId, name, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
    }

    @Test
    @DisplayName("findPage e count passam includeUnverified e includeReaders false")
    void findPageAndCountPassIncludeUnverifiedFalse() {
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = Checker.Type.VERIFY_EMAIL.name();
        when(jpa.findAllAscending(10, false, verifyEmail, false, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(List.of());
        when(jpa.countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, false, verifyEmail, false, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false)).thenReturn(5L);

        assertThat(adapter.findPage(page(null, null, null, 10, false, false))).isEmpty();
        assertThat(adapter.countAfterCursor(page(null, lastSeenName, lastSeenId, 10, false, false))).isEqualTo(5L);
        verify(jpa).findAllAscending(10, false, verifyEmail, false, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
        verify(jpa).countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, false, verifyEmail, false, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), true, false);
    }

    @Test
    @DisplayName("findPage e count passam filtro de type")
    void findPageAndCountPassTypeFilter() {
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = Checker.Type.VERIFY_EMAIL.name();
        final var master = Profile.Type.MASTER.name();
        when(jpa.findAllAscending(10, true, verifyEmail, true, READER, VIEWER_ID, false, master, true, false)).thenReturn(List.of());
        when(jpa.countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, false, master, true, false)).thenReturn(1L);

        final var filtered = new ProfilePageCriteria(
                null, null, null, 10, false, true, true, VIEWER_ID, Profile.Type.MASTER, null);
        final var counted = new ProfilePageCriteria(
                null, lastSeenName, lastSeenId, 10, false, true, true, VIEWER_ID, Profile.Type.MASTER, null);
        assertThat(adapter.findPage(filtered)).isEmpty();
        assertThat(adapter.countAfterCursor(counted)).isEqualTo(1L);
        verify(jpa).findAllAscending(10, true, verifyEmail, true, READER, VIEWER_ID, false, master, true, false);
        verify(jpa).countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, false, master, true, false);
    }

    @Test
    @DisplayName("findPage e count passam filtro de verified")
    void findPageAndCountPassVerifiedFilter() {
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = Checker.Type.VERIFY_EMAIL.name();
        when(jpa.findAllAscending(10, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), false, true))
                .thenReturn(List.of());
        when(jpa.countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), false, false))
                .thenReturn(1L);

        final var filtered = new ProfilePageCriteria(
                null, null, null, 10, false, true, true, VIEWER_ID, null, true);
        final var counted = new ProfilePageCriteria(
                null, lastSeenName, lastSeenId, 10, false, true, true, VIEWER_ID, null, false);
        assertThat(adapter.findPage(filtered)).isEmpty();
        assertThat(adapter.countAfterCursor(counted)).isEqualTo(1L);
        verify(jpa).findAllAscending(10, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), false, true);
        verify(jpa).countForFindAllAscendingAfter(
                lastSeenName, lastSeenId, true, verifyEmail, true, READER, VIEWER_ID, true, Profile.Type.MASTER.name(), false, false);
    }

    private static ProfilePageCriteria page(
            final String name,
            final String lastSeenName,
            final UUID lastSeenId,
            final int limit,
            final boolean reverse,
            final boolean include) {
        return new ProfilePageCriteria(
                name,
                lastSeenName,
                lastSeenId,
                limit,
                reverse,
                include,
                include,
                VIEWER_ID,
                null,
                null);
    }

}
