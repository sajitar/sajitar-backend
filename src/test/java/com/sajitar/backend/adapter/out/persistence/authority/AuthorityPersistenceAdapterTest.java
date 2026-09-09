package com.sajitar.backend.adapter.out.persistence.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityPageCriteria;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorityPersistenceAdapter")
class AuthorityPersistenceAdapterTest {

    @Mock
    private AuthorityJpaRepository jpa;

    private AuthorityPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuthorityPersistenceAdapter(jpa);
    }

    @Test
    @DisplayName("save, find e delete delegam ao JPA")
    void delegatesCrud() {
        final var domain = new Authority(
                UUID.fromString("019c2000-a111-7000-8000-111111111111"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Authority.Type.MASTER);
        final var entity = AuthorityPersistenceMapper.toEntity(domain);
        when(jpa.save(any(AuthorityJpaEntity.class))).thenReturn(entity);
        when(jpa.findById(domain.id())).thenReturn(Optional.of(entity));
        when(jpa.findByProfileIdAndType(domain.profileId(), domain.type())).thenReturn(Optional.of(entity));

        assertThat(adapter.save(domain).id()).isEqualTo(domain.id());
        assertThat(adapter.findById(domain.id())).contains(domain);
        assertThat(adapter.findByProfileIdAndType(domain.profileId(), domain.type())).contains(domain);
        adapter.deleteById(domain.id());
        verify(jpa).deleteById(domain.id());
    }

    @Test
    @DisplayName("findPage sem cursor e com cursor em ASC e DESC")
    void findPageWithAndWithoutCursor() {
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        when(jpa.findPageByProfileId(profileId, 10)).thenReturn(List.of());
        when(jpa.findPageByProfileIdAfter(profileId, (short) 0, 2)).thenReturn(List.of());
        when(jpa.findPageByProfileIdDescending(profileId, 10)).thenReturn(List.of());
        when(jpa.findPageByProfileIdDescendingAfter(profileId, (short) 2, 2)).thenReturn(List.of());

        assertThat(adapter.findPage(new AuthorityPageCriteria(profileId, null, 10, false))).isEmpty();
        assertThat(adapter.findPage(new AuthorityPageCriteria(profileId, Authority.Type.MASTER, 2, false))).isEmpty();
        assertThat(adapter.findPage(new AuthorityPageCriteria(profileId, null, 10, true))).isEmpty();
        assertThat(adapter.findPage(new AuthorityPageCriteria(profileId, Authority.Type.READER, 2, true))).isEmpty();
        verify(jpa).findPageByProfileId(profileId, 10);
        verify(jpa).findPageByProfileIdAfter(profileId, (short) 0, 2);
        verify(jpa).findPageByProfileIdDescending(profileId, 10);
        verify(jpa).findPageByProfileIdDescendingAfter(profileId, (short) 2, 2);
    }

    @Test
    @DisplayName("countAfterCursor ASC, DESC e sem cursor")
    void countAfterCursorDirections() {
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        when(jpa.countByProfileIdAndTypeAfter(profileId, (short) 0)).thenReturn(2L);
        when(jpa.countByProfileIdAndTypeBefore(profileId, (short) 1)).thenReturn(1L);

        assertThat(adapter.countAfterCursor(new AuthorityPageCriteria(profileId, null, 10, false))).isZero();
        assertThat(adapter.countAfterCursor(
                new AuthorityPageCriteria(profileId, Authority.Type.MASTER, 10, false))).isEqualTo(2L);
        assertThat(adapter.countAfterCursor(
                new AuthorityPageCriteria(profileId, Authority.Type.MEMBER, 10, true))).isEqualTo(1L);
        verify(jpa).countByProfileIdAndTypeAfter(profileId, (short) 0);
        verify(jpa).countByProfileIdAndTypeBefore(profileId, (short) 1);
    }

}
