package com.sajitar.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MailLayout")
class MailLayoutTest {

    @Test
    @DisplayName("Preenche o HTML do classpath e agrupa o código em dois blocos")
    void rendersTemplateFromClasspath() {
        final var html = MailLayout.render(
                "pt",
                "Title",
                "preheader 123456",
                "Heading",
                "Lead 123456",
                "123456",
                "Hint",
                "Footer");

        assertThat(html).contains("<!DOCTYPE html");
        assertThat(html).contains("lang=\"pt\"");
        assertThat(html).contains("<title>Title</title>");
        assertThat(html).contains("preheader 123456");
        assertThat(html).contains("Heading");
        assertThat(html).contains("Lead 123456");
        assertThat(html).contains("123&nbsp;456");
        assertThat(html).contains("Hint");
        assertThat(html).contains("Footer");
        assertThat(html).doesNotContain("{{");
    }

    @Test
    @DisplayName("Locale vazio cai no inglês; valores nulos viram texto vazio")
    void defaultsBlankLangAndNullCopy() {
        final var html = MailLayout.render(null, null, null, null, null, null, null, null);

        assertThat(html).contains("lang=\"en\"");
        assertThat(html).contains("<title></title>");
        assertThat(html).doesNotContain("{{");
        assertThat(MailLayout.render("  ", "t", "p", "h", "l", "123456", "i", "f")).contains("lang=\"en\"");
    }

    @Test
    @DisplayName("Escapa HTML na copy e não parte código que não tem 6 dígitos")
    void escapesCopyAndLeavesIrregularCode() {
        final var html = MailLayout.render(
                "en",
                "Title",
                "a < b & c > \"d\"",
                "Heading",
                "Lead",
                "12",
                "Hint",
                "Footer");

        assertThat(html).contains("a &lt; b &amp; c &gt; &quot;d&quot;");
        assertThat(html).contains(">12</p>");
        assertThat(html).doesNotContain("&nbsp;");
    }

    @Test
    @DisplayName("Instante do assunto é dia, hora, minuto e segundo em UTC")
    void formatsSubjectInstantInUtc() {
        assertThat(MailLayout.subjectWhen(Instant.parse("2026-09-20T21:28:03Z")))
                .isEqualTo("2026-09-20 21:28:03 UTC");
    }

    @Test
    @DisplayName("Arquivo ausente lança IllegalStateException")
    void missingTemplateThrows() {
        final var thrown = catchThrowable(() -> MailLayout.read(null));

        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");
    }

    @Test
    @DisplayName("Falha de leitura lança IllegalStateException")
    void unreadableTemplateThrows() {
        final var stream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        final var thrown = catchThrowable(() -> MailLayout.read(stream));

        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unreadable")
                .hasCauseInstanceOf(IOException.class);
    }

}
