package com.sajitar.backend.domain.model.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sajitar.backend.domain.exception.InvalidTermLangException;
import com.sajitar.backend.domain.exception.InvalidTermTypeException;

@DisplayName("TermDetail (linha)")
class TermDetailTest {

    @Test
    @DisplayName("create preserva termId, type e content e nasce com draft true")
    void createStartsAsDraft() {
        final var termId = UUID.randomUUID();
        final var created = TermDetail.create(termId, TermDetail.Type.EN, "Hello.");

        assertThat(created.termId()).isEqualTo(termId);
        assertThat(created.type()).isEqualTo(TermDetail.Type.EN);
        assertThat(created.content()).isEqualTo("Hello.");
        assertThat(created.draft()).isTrue();
    }

    @Test
    @DisplayName("withContent copia termId, type e draft")
    void withContentCopiesRemainingFields() {
        final var original = TermDetail.create(UUID.randomUUID(), TermDetail.Type.EN, "Hello.");
        final var updated = original.withContent("Hi.");

        assertThat(updated.termId()).isEqualTo(original.termId());
        assertThat(updated.type()).isEqualTo(original.type());
        assertThat(updated.draft()).isEqualTo(original.draft());
        assertThat(updated.content()).isEqualTo("Hi.");
    }

    @Test
    @DisplayName("withDraft copia termId, type e content")
    void withDraftCopiesRemainingFields() {
        final var original = TermDetail.create(UUID.randomUUID(), TermDetail.Type.EN, "Hello.");
        final var updated = original.withDraft(false);

        assertThat(updated.termId()).isEqualTo(original.termId());
        assertThat(updated.type()).isEqualTo(original.type());
        assertThat(updated.content()).isEqualTo(original.content());
        assertThat(updated.draft()).isFalse();
    }

    @Test
    @DisplayName("equals considera termId e type e rejeita outros tipos")
    void equalsByTermIdAndType() {
        final var termId = UUID.randomUUID();
        final var a = new TermDetail(termId, TermDetail.Type.EN, "a", true);
        final var b = new TermDetail(termId, TermDetail.Type.EN, "b", false);
        final var differentType = new TermDetail(termId, TermDetail.Type.PT, "a", true);
        final var differentId = new TermDetail(UUID.randomUUID(), TermDetail.Type.EN, "a", true);

        assertThat(a).isEqualTo(b)
                .isNotEqualTo(differentType)
                .isNotEqualTo(differentId)
                .isNotEqualTo("nao-e-term-detail")
                .isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @ParameterizedTest
    @EnumSource(TermDetail.Type.class)
    @DisplayName("Type.parse aceita o nome do enum")
    void parseAcceptsEnumName(final TermDetail.Type expected) {
        assertThat(TermDetail.Type.parse(expected.name())).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "pt", "es", "FR", "", "0" })
    @DisplayName("Type.parse rejeita nomes desconhecidos")
    void parseRejectsUnknownNames(final String raw) {
        final var thrown = catchThrowable(() -> TermDetail.Type.parse(raw));
        assertThat(thrown).isInstanceOf(InvalidTermTypeException.class);
        assertThat(((InvalidTermTypeException) thrown).rejectedValue()).isEqualTo(raw);
    }

    @Test
    @DisplayName("Type.parse rejeita nulo")
    void parseRejectsNull() {
        final var thrown = catchThrowable(() -> TermDetail.Type.parse(null));
        assertThat(thrown).isInstanceOf(InvalidTermTypeException.class);
        assertThat(((InvalidTermTypeException) thrown).rejectedValue()).isEqualTo("null");
    }

    @ParameterizedTest
    @CsvSource({ "en,EN", "pt,PT", "es,ES" })
    @DisplayName("Type.fromLang mapeia en, pt e es")
    void fromLangMapsSupportedLanguages(final String lang, final TermDetail.Type expected) {
        assertThat(TermDetail.Type.fromLang(lang)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "EN", "fr", "", "english" })
    @DisplayName("Type.fromLang rejeita idiomas desconhecidos")
    void fromLangRejectsUnknownLanguages(final String lang) {
        final var thrown = catchThrowable(() -> TermDetail.Type.fromLang(lang));
        assertThat(thrown).isInstanceOf(InvalidTermLangException.class);
        assertThat(((InvalidTermLangException) thrown).rejectedValue()).isEqualTo(lang);
    }

    @Test
    @DisplayName("Type.fromLang rejeita nulo")
    void fromLangRejectsNull() {
        final var thrown = catchThrowable(() -> TermDetail.Type.fromLang(null));
        assertThat(thrown).isInstanceOf(InvalidTermLangException.class);
        assertThat(((InvalidTermLangException) thrown).rejectedValue()).isEqualTo("null");
    }

}
