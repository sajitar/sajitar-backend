package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

@DisplayName("MailProperties")
class MailPropertiesTest {

    @Test
    @DisplayName("Aceita host, porta SMTP e remetente preenchidos")
    void acceptsValidConfiguration() {
        final var properties = MailPropertiesFixture.defaults();

        assertThat(properties.host()).isEqualTo(MailPropertiesFixture.HOST);
        assertThat(properties.port()).isEqualTo(MailPropertiesFixture.PORT);
        assertThat(properties.from()).isEqualTo(MailPropertiesFixture.FROM);
        assertThat(properties.smtpAuth()).isFalse();
        assertThat(properties.startTls()).isFalse();
        assertThat(properties.username()).isEmpty();
        assertThat(properties.password()).isEmpty();
    }

    @Test
    @DisplayName("Liga host, porta e remetente pelo prefixo sajitar.mail")
    void bindsHostPortAndFrom() {
        final var source = new MapConfigurationPropertySource(Map.of(
                "sajitar.mail.host", MailPropertiesFixture.HOST,
                "sajitar.mail.port", String.valueOf(MailPropertiesFixture.PORT),
                "sajitar.mail.from", MailPropertiesFixture.FROM));

        final var properties = new Binder(source).bind("sajitar.mail", Bindable.of(MailProperties.class)).get();

        assertThat(properties.host()).isEqualTo(MailPropertiesFixture.HOST);
        assertThat(properties.port()).isEqualTo(MailPropertiesFixture.PORT);
        assertThat(properties.from()).isEqualTo(MailPropertiesFixture.FROM);
        assertThat(properties.smtpAuth()).isFalse();
        assertThat(properties.startTls()).isFalse();
    }

    @Test
    @DisplayName("Liga usuário, senha e STARTTLS")
    void bindsAuthAndStartTls() {
        final var source = new MapConfigurationPropertySource(Map.of(
                "sajitar.mail.host", MailPropertiesFixture.HOST,
                "sajitar.mail.port", String.valueOf(MailPropertiesFixture.PORT),
                "sajitar.mail.from", MailPropertiesFixture.FROM,
                "sajitar.mail.username", "smtp-user",
                "sajitar.mail.password", "smtp-pass",
                "sajitar.mail.start-tls", "true"));

        final var properties = new Binder(source).bind("sajitar.mail", Bindable.of(MailProperties.class)).get();

        assertThat(properties.smtpAuth()).isTrue();
        assertThat(properties.username()).isEqualTo("smtp-user");
        assertThat(properties.password()).isEqualTo("smtp-pass");
        assertThat(properties.startTls()).isTrue();
    }

    @Test
    @DisplayName("Username preenchido liga autenticação SMTP")
    void enablesSmtpAuthWhenUsernamePresent() {
        final var properties = new MailProperties(MailPropertiesFixture.HOST, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM, "smtp-user", "smtp-pass", true);

        assertThat(properties.smtpAuth()).isTrue();
        assertThat(properties.username()).isEqualTo("smtp-user");
        assertThat(properties.password()).isEqualTo("smtp-pass");
        assertThat(properties.startTls()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Username em branco desliga auth e STARTTLS")
    void blankUsernameDisablesAuthAndStartTls(final String username) {
        final var properties = new MailProperties(MailPropertiesFixture.HOST, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM, username, "kept", true);

        assertThat(properties.smtpAuth()).isFalse();
        assertThat(properties.username()).isEmpty();
        assertThat(properties.password()).isEqualTo("kept");
        assertThat(properties.startTls()).isFalse();
    }

    @Test
    @DisplayName("Password nula sem username vira string vazia")
    void nullPasswordWithoutUsernameBecomesEmpty() {
        final var properties = new MailProperties(MailPropertiesFixture.HOST, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM, null, null, true);

        assertThat(properties.smtpAuth()).isFalse();
        assertThat(properties.username()).isEmpty();
        assertThat(properties.password()).isEmpty();
        assertThat(properties.startTls()).isFalse();
    }

    @Test
    @DisplayName("Password nula com username vira string vazia")
    void nullPasswordWithUsernameBecomesEmpty() {
        final var properties = new MailProperties(MailPropertiesFixture.HOST, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM, "smtp-user", null, false);

        assertThat(properties.smtpAuth()).isTrue();
        assertThat(properties.password()).isEmpty();
        assertThat(properties.startTls()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita host em branco")
    void rejectsBlankHost(final String host) {
        final var thrown = catchThrowable(() -> new MailProperties(host, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, 65536 })
    @DisplayName("Rejeita porta fora de 1..65535")
    void rejectsPortOutOfRange(final int port) {
        final var thrown = catchThrowable(() -> new MailProperties(MailPropertiesFixture.HOST, port,
                MailPropertiesFixture.FROM));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita remetente em branco")
    void rejectsBlankFrom(final String from) {
        final var thrown = catchThrowable(() -> new MailProperties(MailPropertiesFixture.HOST,
                MailPropertiesFixture.PORT, from));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

}
