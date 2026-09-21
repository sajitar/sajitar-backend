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
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                "user@example.com",
                "hashed-password");
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
        when(jpa.findUnverifiedCreatedBefore((short) Checker.Type.VERIFY_EMAIL.value(), cutoffId))
                .thenReturn(List.of(profileId));

        assertThat(adapter.findUnverifiedCreatedBefore(cutoff)).containsExactly(profileId);
        verify(jpa).findUnverifiedCreatedBefore((short) 1, cutoffId);
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
        final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
        when(jpa.findAllAscending(10, true, verifyEmail)).thenReturn(List.of());
        when(jpa.findAllAscendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail)).thenReturn(List.of());
        when(jpa.findAllDescending(10, true, verifyEmail)).thenReturn(List.of());
        when(jpa.findAllDescendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail)).thenReturn(List.of());

        assertThat(adapter.findPage(new ProfilePageCriteria(null, null, null, 10, false, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(null, lastSeenName, lastSeenId, 2, false, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(null, null, null, 10, true, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(null, lastSeenName, lastSeenId, 2, true, true))).isEmpty();
        verify(jpa).findAllAscending(10, true, verifyEmail);
        verify(jpa).findAllAscendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail);
        verify(jpa).findAllDescending(10, true, verifyEmail);
        verify(jpa).findAllDescendingAfter(2, lastSeenName, lastSeenId, true, verifyEmail);
    }

    @Test
    @DisplayName("findPage com filtro de nome: ASC e DESC com e sem cursor")
    void findPageWithNameFilter() {
        final var name = "Silva";
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
        when(jpa.findByNameContainingIgnoreCaseAscending(10, name, true, verifyEmail)).thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseAscendingAfter(2, lastSeenName, lastSeenId, name, true, verifyEmail))
                .thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseDescending(10, name, true, verifyEmail)).thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseDescendingAfter(2, lastSeenName, lastSeenId, name, true, verifyEmail))
                .thenReturn(List.of());

        assertThat(adapter.findPage(new ProfilePageCriteria(name, null, null, 10, false, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(name, lastSeenName, lastSeenId, 2, false, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(name, null, null, 10, true, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(name, lastSeenName, lastSeenId, 2, true, true))).isEmpty();
        verify(jpa).findByNameContainingIgnoreCaseAscending(10, name, true, verifyEmail);
        verify(jpa).findByNameContainingIgnoreCaseAscendingAfter(2, lastSeenName, lastSeenId, name, true, verifyEmail);
        verify(jpa).findByNameContainingIgnoreCaseDescending(10, name, true, verifyEmail);
        verify(jpa).findByNameContainingIgnoreCaseDescendingAfter(
                2,
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmail);
    }

    @Test
    @DisplayName("countAfterCursor com e sem filtro de nome em ASC e DESC")
    void countAfterCursorDirections() {
        final var name = "Silva";
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
        when(jpa.countForFindAllAscendingAfter(lastSeenName, lastSeenId, true, verifyEmail)).thenReturn(2L);
        when(jpa.countForFindAllDescendingAfter(lastSeenName, lastSeenId, true, verifyEmail)).thenReturn(1L);
        when(jpa.countForFindByNameContainingIgnoreCaseAscendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmail)).thenReturn(3L);
        when(jpa.countForFindByNameContainingIgnoreCaseDescendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmail)).thenReturn(4L);

        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(null, lastSeenName, lastSeenId, 10, false, true))).isEqualTo(2L);
        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(null, lastSeenName, lastSeenId, 10, true, true))).isEqualTo(1L);
        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(name, lastSeenName, lastSeenId, 10, false, true))).isEqualTo(3L);
        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(name, lastSeenName, lastSeenId, 10, true, true))).isEqualTo(4L);
        verify(jpa).countForFindAllAscendingAfter(lastSeenName, lastSeenId, true, verifyEmail);
        verify(jpa).countForFindAllDescendingAfter(lastSeenName, lastSeenId, true, verifyEmail);
        verify(jpa).countForFindByNameContainingIgnoreCaseAscendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmail);
        verify(jpa).countForFindByNameContainingIgnoreCaseDescendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmail);
    }

    @Test
    @DisplayName("findPage e count passam includeUnverified false")
    void findPageAndCountPassIncludeUnverifiedFalse() {
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
        when(jpa.findAllAscending(10, false, verifyEmail)).thenReturn(List.of());
        when(jpa.countForFindAllAscendingAfter(lastSeenName, lastSeenId, false, verifyEmail)).thenReturn(5L);

        assertThat(adapter.findPage(new ProfilePageCriteria(null, null, null, 10, false, false))).isEmpty();
        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(null, lastSeenName, lastSeenId, 10, false, false))).isEqualTo(5L);
        verify(jpa).findAllAscending(10, false, verifyEmail);
        verify(jpa).countForFindAllAscendingAfter(lastSeenName, lastSeenId, false, verifyEmail);
    }

}
