package com.sajitar.backend.adapter.out.persistence.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("findPage sem filtro de nome: ASC e DESC com e sem cursor")
    void findPageWithoutNameFilter() {
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        when(jpa.findAllAscending(10)).thenReturn(List.of());
        when(jpa.findAllAscendingAfter(2, lastSeenName, lastSeenId)).thenReturn(List.of());
        when(jpa.findAllDescending(10)).thenReturn(List.of());
        when(jpa.findAllDescendingAfter(2, lastSeenName, lastSeenId)).thenReturn(List.of());

        assertThat(adapter.findPage(new ProfilePageCriteria(null, null, null, 10, false))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(null, lastSeenName, lastSeenId, 2, false))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(null, null, null, 10, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(null, lastSeenName, lastSeenId, 2, true))).isEmpty();
        verify(jpa).findAllAscending(10);
        verify(jpa).findAllAscendingAfter(2, lastSeenName, lastSeenId);
        verify(jpa).findAllDescending(10);
        verify(jpa).findAllDescendingAfter(2, lastSeenName, lastSeenId);
    }

    @Test
    @DisplayName("findPage com filtro de nome: ASC e DESC com e sem cursor")
    void findPageWithNameFilter() {
        final var name = "Silva";
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        when(jpa.findByNameContainingIgnoreCaseAscending(10, name)).thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseAscendingAfter(2, lastSeenName, lastSeenId, name))
                .thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseDescending(10, name)).thenReturn(List.of());
        when(jpa.findByNameContainingIgnoreCaseDescendingAfter(2, lastSeenName, lastSeenId, name))
                .thenReturn(List.of());

        assertThat(adapter.findPage(new ProfilePageCriteria(name, null, null, 10, false))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(name, lastSeenName, lastSeenId, 2, false))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(name, null, null, 10, true))).isEmpty();
        assertThat(adapter.findPage(new ProfilePageCriteria(name, lastSeenName, lastSeenId, 2, true))).isEmpty();
        verify(jpa).findByNameContainingIgnoreCaseAscending(10, name);
        verify(jpa).findByNameContainingIgnoreCaseAscendingAfter(2, lastSeenName, lastSeenId, name);
        verify(jpa).findByNameContainingIgnoreCaseDescending(10, name);
        verify(jpa).findByNameContainingIgnoreCaseDescendingAfter(2, lastSeenName, lastSeenId, name);
    }

    @Test
    @DisplayName("countAfterCursor com e sem filtro de nome em ASC e DESC")
    void countAfterCursorDirections() {
        final var name = "Silva";
        final var lastSeenName = "Maria Silva";
        final var lastSeenId = UUID.fromString("019c0000-a111-7000-8000-111111111111");
        when(jpa.countForFindAllAscendingAfter(lastSeenName, lastSeenId)).thenReturn(2L);
        when(jpa.countForFindAllDescendingAfter(lastSeenName, lastSeenId)).thenReturn(1L);
        when(jpa.countForFindByNameContainingIgnoreCaseAscendingAfter(lastSeenName, lastSeenId, name))
                .thenReturn(3L);
        when(jpa.countForFindByNameContainingIgnoreCaseDescendingAfter(lastSeenName, lastSeenId, name))
                .thenReturn(4L);

        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(null, lastSeenName, lastSeenId, 10, false))).isEqualTo(2L);
        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(null, lastSeenName, lastSeenId, 10, true))).isEqualTo(1L);
        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(name, lastSeenName, lastSeenId, 10, false))).isEqualTo(3L);
        assertThat(adapter.countAfterCursor(
                new ProfilePageCriteria(name, lastSeenName, lastSeenId, 10, true))).isEqualTo(4L);
        verify(jpa).countForFindAllAscendingAfter(lastSeenName, lastSeenId);
        verify(jpa).countForFindAllDescendingAfter(lastSeenName, lastSeenId);
        verify(jpa).countForFindByNameContainingIgnoreCaseAscendingAfter(lastSeenName, lastSeenId, name);
        verify(jpa).countForFindByNameContainingIgnoreCaseDescendingAfter(lastSeenName, lastSeenId, name);
    }

}
