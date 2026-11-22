package com.sajitar.backend.application.usecase.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.term.PatchTermCommand;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.InvalidTermDraftException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.exception.TermTypeAbsentException;
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchTermUseCase")
class PatchTermUseCaseTest {

    @Mock
    private TermRepository terms;

    @Mock
    private ProfileRepository profiles;

    private PatchTermUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchTermUseCase(terms, profiles, TermUseCaseFixture.VALIDATOR);
    }

    private void withMaster() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
    }

    private static PatchTermCommand command(
            final TermDetail.Type type, final PatchValue<String> content, final PatchValue<Boolean> draft) {
        return new PatchTermCommand(TermUseCaseFixture.TERM_ID, type, content, draft);
    }

    private static List<TermDetail> openDraft() {
        return List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.EN));
    }

    private static List<TermDetail> allDrafts() {
        return List.of(
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.EN),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.PT),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.ES));
    }

    private static List<TermDetail> publishedWithDraftSiblings() {
        return List.of(
                TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, TermDetail.Type.EN),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.PT),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.ES));
    }

    private static List<TermDetail> allPublished() {
        return List.of(
                TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, TermDetail.Type.EN),
                TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, TermDetail.Type.PT),
                TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, TermDetail.Type.ES));
    }

    @Test
    @DisplayName("content presente e draft ausente alteram o texto e mantêm draft true")
    void patchesContentKeepingDraft() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Novo texto."), PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.content()).isEqualTo("Novo texto.");
        assertThat(saved.draft()).isTrue();
        assertThat(saved.type()).isEqualTo(TermDetail.Type.EN);
    }

    @Test
    @DisplayName("draft nulo no construtor vira ausente e mantém draft true")
    void normalizesNullDraft() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Texto."), null),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.content()).isEqualTo("Texto.");
        assertThat(saved.draft()).isTrue();
    }

    @Test
    @DisplayName("content nulo no construtor vira ausente e mantém o texto")
    void normalizesNullContent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, null, PatchValue.of(true)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.content()).isEqualTo("Rascunho EN");
        assertThat(saved.draft()).isTrue();
    }

    @Test
    @DisplayName("draft true no rascunho mantém draft true")
    void keepsDraftTrue() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Texto."), PatchValue.of(true)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.content()).isEqualTo("Texto.");
        assertThat(saved.draft()).isTrue();
    }

    @Test
    @DisplayName("draft false com EN, PT e ES publica a linha e grava o texto")
    void publishesWhenAllTypesPresent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(allDrafts());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Final."), PatchValue.of(false)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isFalse();
        assertThat(saved.content()).isEqualTo("Final.");
        assertThat(saved.type()).isEqualTo(TermDetail.Type.EN);
    }

    @Test
    @DisplayName("draft false com content ausente publica e mantém o texto")
    void publishesKeepingContent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(allDrafts());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.absent(), PatchValue.of(false)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isFalse();
        assertThat(saved.content()).isEqualTo("Rascunho EN");
    }

    @Test
    @DisplayName("linha publicada com irmã em rascunho mantém draft false quando draft é omitido")
    void keepsPublishedDraftWhenOmitted() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(publishedWithDraftSiblings());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Novo publicado."), PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isFalse();
        assertThat(saved.content()).isEqualTo("Novo publicado.");
    }

    @Test
    @DisplayName("linha publicada com irmã em rascunho mantém draft false quando o corpo repete false")
    void keepsPublishedDraftWhenRepeated() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(publishedWithDraftSiblings());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.absent(), PatchValue.of(false)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isFalse();
        assertThat(saved.content()).isEqualTo("Publicado EN");
    }

    @Test
    @DisplayName("linha publicada com irmã em rascunho reabre com draft true")
    void reopensPublishedLine() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(publishedWithDraftSiblings());
        when(terms.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Reaberto."), PatchValue.of(true)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isTrue();
        assertThat(saved.content()).isEqualTo("Reaberto.");
    }

    @Test
    @DisplayName("perfil sem MASTER lança ForbiddenTermException e não lê o termo")
    void forbiddenWhenNotMaster() {
        when(profiles.findById(TermUseCaseFixture.READER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.readerProfile()));

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.absent(), PatchValue.absent()),
                TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).findByIdAll(any());
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("perfil inexistente lança ForbiddenTermException e não lê o termo")
    void forbiddenWhenProfileMissing() {
        when(profiles.findById(TermUseCaseFixture.READER_ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Texto."), PatchValue.absent()),
                TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).findByIdAll(any());
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("id ausente lança TermNotFoundException antes do corpo")
    void throwsWhenIdMissing() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.absent(), PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermNotFoundException.class);
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("linha do type ausente lança TermTypeAbsentException antes do corpo")
    void throwsWhenTypeAbsent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.PT, PatchValue.of("  "), PatchValue.of(null)),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermTypeAbsentException.class);
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("id fechado recusa o corpo válido com draft closed")
    void throwsWhenIdClosed() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(allPublished());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Texto."), PatchValue.of(true)),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(InvalidTermDraftException.class);
        assertThat(((InvalidTermDraftException) thrown).content().get("draft"))
                .containsExactly(InvalidTermDraftException.CLOSED_KEY);
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("id fechado conserva draft closed quando o corpo é inválido")
    void closedWinsOverInvalidBody() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(allPublished());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("  "), PatchValue.of(null)),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(InvalidTermDraftException.class);
        assertThat(((InvalidTermDraftException) thrown).content().get("draft"))
                .containsExactly(InvalidTermDraftException.CLOSED_KEY);
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("draft false sem os três type recusa com draft incomplete")
    void throwsWhenPublishIncomplete() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.EN),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.PT)));

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.absent(), PatchValue.of(false)),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(InvalidTermDraftException.class);
        assertThat(((InvalidTermDraftException) thrown).content().get("draft"))
                .containsExactly(InvalidTermDraftException.INCOMPLETE_KEY);
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("corpo vazio responde content e draft")
    void rejectsEmptyBody() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.absent(), PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertViolationTemplates(thrown, "content", "{validation.not-blank}", "draft", "{validation.not-null}");
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("content em branco responde só content")
    void rejectsBlankContent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("  "), PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertViolationTemplates(thrown, "content", "{validation.not-blank}");
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("content nulo responde só content quando draft é válido")
    void rejectsNullContent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of(null), PatchValue.of(true)),
                TermUseCaseFixture.MASTER_ID));

        assertViolationTemplates(thrown, "content", "{validation.not-blank}");
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("draft nulo responde só draft quando content é válido")
    void rejectsNullDraft() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of("Texto."), PatchValue.of(null)),
                TermUseCaseFixture.MASTER_ID));

        assertViolationTemplates(thrown, "draft", "{validation.not-null}");
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("draft nulo responde só draft quando content está ausente")
    void rejectsNullDraftWithAbsentContent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.absent(), PatchValue.of(null)),
                TermUseCaseFixture.MASTER_ID));

        assertViolationTemplates(thrown, "draft", "{validation.not-null}");
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("content inválido e draft nulo respondem as duas chaves")
    void rejectsInvalidContentAndNullDraft() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of(""), PatchValue.of(null)),
                TermUseCaseFixture.MASTER_ID));

        assertViolationTemplates(thrown, "content", "{validation.not-blank}", "draft", "{validation.not-null}");
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("content inválido com draft false responde só content")
    void rejectsBlankContentBeforeIncompletePublish() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(openDraft());

        final var thrown = catchThrowable(() -> useCase.execute(
                command(TermDetail.Type.EN, PatchValue.of(""), PatchValue.of(false)),
                TermUseCaseFixture.MASTER_ID));

        assertViolationTemplates(thrown, "content", "{validation.not-blank}");
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("id nulo não consulta repositórios")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(null, TermDetail.Type.EN, PatchValue.of("Texto."), PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("type nulo não consulta repositórios")
    void rejectsNullType() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, null, PatchValue.of("Texto."), PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
        verify(terms, never()).save(any(), any());
    }

    private static void assertViolationTemplates(final Throwable thrown, final String... propertyThenTemplate) {
        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        assertThat(propertyThenTemplate.length % 2).isZero();
        final var expected = new ArrayList<Tuple>();
        for (var index = 0; index < propertyThenTemplate.length; index += 2) {
            expected.add(tuple(propertyThenTemplate[index], propertyThenTemplate[index + 1]));
        }
        assertThat(((ConstraintViolationException) thrown).getConstraintViolations())
                .extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessageTemplate)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

}
