package com.sajitar.backend.adapter.out.persistence.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TermPersistenceAdapter")
class TermPersistenceAdapterTest {

    @Mock
    private TermJpaRepository jpa;

    private TermPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TermPersistenceAdapter(jpa);
    }

    @Test
    @DisplayName("findCurrent devolve o termo de maior id")
    void findCurrentReturnsLatest() {
        final var entity = TermJpaEntity.builder()
                .id(UUID.fromString("019c4000-a222-7000-8000-222222222222"))
                .content("Termo vigente.")
                .build();
        when(jpa.findFirstByOrderByIdDesc()).thenReturn(Optional.of(entity));

        final var found = adapter.findCurrent();

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(entity.getId());
        assertThat(found.get().content()).isEqualTo("Termo vigente.");
        verify(jpa).findFirstByOrderByIdDesc();
    }

    @Test
    @DisplayName("findCurrent vazio quando não há termo")
    void findCurrentEmpty() {
        when(jpa.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());

        assertThat(adapter.findCurrent()).isEmpty();
    }

}
