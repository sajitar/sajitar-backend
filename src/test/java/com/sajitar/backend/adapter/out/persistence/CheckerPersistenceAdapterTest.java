package com.sajitar.backend.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerPageCriteria;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckerPersistenceAdapter")
class CheckerPersistenceAdapterTest {

    @Mock
    private CheckerJpaRepository jpa;

    private CheckerPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CheckerPersistenceAdapter(jpa);
    }

    @Test
    @DisplayName("save, find e delete delegam ao JPA")
    void delegatesCrud() {
        final var domain = new Checker(
                UUID.fromString("019c1000-a111-7000-8000-111111111111"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Checker.Type.CHANGE_EMAIL,
                "123456",
                null,
                10,
                3,
                Instant.parse("2001-04-24T21:00:00Z"));
        final var entity = CheckerPersistenceMapper.toEntity(domain);
        when(jpa.save(any(CheckerJpaEntity.class))).thenReturn(entity);
        when(jpa.findById(domain.id())).thenReturn(Optional.of(entity));
        when(jpa.findByProfileIdAndType(domain.profileId(), domain.type())).thenReturn(Optional.of(entity));

        assertThat(adapter.save(domain).id()).isEqualTo(domain.id());
        assertThat(adapter.findById(domain.id())).contains(domain);
        assertThat(adapter.findByProfileIdAndType(domain.profileId(), domain.type())).contains(domain);
        adapter.deleteById(domain.id());
        verify(jpa).deleteById(domain.id());
    }

    @Test
    @DisplayName("findPage sem cursor e com cursor")
    void findPageWithAndWithoutCursor() {
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        when(jpa.findPageByProfileId(profileId, 10)).thenReturn(List.of());
        when(jpa.findPageByProfileIdAfter(profileId, (short) 0, 2)).thenReturn(List.of());

        assertThat(adapter.findPage(new CheckerPageCriteria(profileId, null, 10))).isEmpty();
        assertThat(adapter.findPage(new CheckerPageCriteria(profileId, Checker.Type.CHANGE_EMAIL, 2))).isEmpty();
        verify(jpa).findPageByProfileId(profileId, 10);
        verify(jpa).findPageByProfileIdAfter(profileId, (short) 0, 2);
    }

}
