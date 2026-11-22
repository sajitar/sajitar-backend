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

@DisplayName("Term (agregado)")
class TermTest {

    @Test
    @DisplayName("create gera id, preserva type e content e nasce com draft true")
    void createGeneratesId() {
        final var created = Term.create(Term.Type.EN, "Hello.");

        assertThat(created.id()).isNotNull();
        assertThat(created.type()).isEqualTo(Term.Type.EN);
        assertThat(created.content()).isEqualTo("Hello.");
        assertThat(created.draft()).isTrue();
    }

    @Test
    @DisplayName("withContent copia id, type e draft")
    void withContentCopiesRemainingFields() {
        final var original = Term.create(Term.Type.EN, "Hello.");
        final var updated = original.withContent("Hi.");

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.type()).isEqualTo(original.type());
        assertThat(updated.draft()).isEqualTo(original.draft());
        assertThat(updated.content()).isEqualTo("Hi.");
    }

    @Test
    @DisplayName("withDraft copia id, type e content")
    void withDraftCopiesRemainingFields() {
        final var original = Term.create(Term.Type.EN, "Hello.");
        final var updated = original.withDraft(false);

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.type()).isEqualTo(original.type());
        assertThat(updated.content()).isEqualTo(original.content());
        assertThat(updated.draft()).isFalse();
    }

    @Test
    @DisplayName("equals considera id e type e rejeita outros tipos")
    void equalsByIdAndType() {
        final var id = UUID.randomUUID();
        final var a = new Term(id, Term.Type.EN, "a", true);
        final var b = new Term(id, Term.Type.EN, "b", false);
        final var differentType = new Term(id, Term.Type.PT, "a", true);
        final var differentId = new Term(UUID.randomUUID(), Term.Type.EN, "a", true);

        assertThat(a).isEqualTo(b)
                .isNotEqualTo(differentType)
                .isNotEqualTo(differentId)
                .isNotEqualTo("nao-e-term")
                .isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @ParameterizedTest
    @EnumSource(Term.Type.class)
    @DisplayName("Type.parse aceita o nome do enum")
    void parseAcceptsEnumName(final Term.Type expected) {
        assertThat(Term.Type.parse(expected.name())).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "pt", "es", "FR", "", "0" })
    @DisplayName("Type.parse rejeita nomes desconhecidos")
    void parseRejectsUnknownNames(final String raw) {
        final var thrown = catchThrowable(() -> Term.Type.parse(raw));
        assertThat(thrown).isInstanceOf(InvalidTermTypeException.class);
        assertThat(((InvalidTermTypeException) thrown).rejectedValue()).isEqualTo(raw);
    }

    @Test
    @DisplayName("Type.parse rejeita nulo")
    void parseRejectsNull() {
        final var thrown = catchThrowable(() -> Term.Type.parse(null));
        assertThat(thrown).isInstanceOf(InvalidTermTypeException.class);
        assertThat(((InvalidTermTypeException) thrown).rejectedValue()).isEqualTo("null");
    }

    @ParameterizedTest
    @CsvSource({ "en,EN", "pt,PT", "es,ES" })
    @DisplayName("Type.fromLang mapeia en, pt e es")
    void fromLangMapsSupportedLanguages(final String lang, final Term.Type expected) {
        assertThat(Term.Type.fromLang(lang)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "EN", "fr", "", "english" })
    @DisplayName("Type.fromLang rejeita idiomas desconhecidos")
    void fromLangRejectsUnknownLanguages(final String lang) {
        final var thrown = catchThrowable(() -> Term.Type.fromLang(lang));
        assertThat(thrown).isInstanceOf(InvalidTermLangException.class);
        assertThat(((InvalidTermLangException) thrown).rejectedValue()).isEqualTo(lang);
    }

    @Test
    @DisplayName("Type.fromLang rejeita nulo")
    void fromLangRejectsNull() {
        final var thrown = catchThrowable(() -> Term.Type.fromLang(null));
        assertThat(thrown).isInstanceOf(InvalidTermLangException.class);
        assertThat(((InvalidTermLangException) thrown).rejectedValue()).isEqualTo("null");
    }

}
