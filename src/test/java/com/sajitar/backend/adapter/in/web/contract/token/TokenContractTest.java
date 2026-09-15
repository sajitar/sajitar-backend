package com.sajitar.backend.adapter.in.web.contract.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.token.IssuedSession;
import com.sajitar.backend.domain.model.token.IssuedToken;
import com.sajitar.backend.domain.model.token.TokenClaims;
import com.sajitar.backend.domain.model.token.TokenUse;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("Contratos de /tokens")
class TokenContractTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final UUID SESSION_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000010");

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("SignInRequest converte credenciais e o opt-in de refresh para o command")
    void signInRequestBecomesCommand() {
        final var command = new SignInRequest("alice@example.com", "senhaSegura1", true).toCommand();

        assertThat(command.email()).isEqualTo("alice@example.com");
        assertThat(command.password()).isEqualTo("senhaSegura1");
        assertThat(command.refresh()).isTrue();
    }

    @Test
    @DisplayName("SignInRequest sem refresh mantém o padrão de sessão só com access")
    void signInRequestDefaultsToAccessOnly() {
        assertThat(new SignInRequest("alice@example.com", "senhaSegura1", false).toCommand().refresh()).isFalse();
    }

    @Test
    @DisplayName("RefreshRequest converte o token para o command")
    void refreshRequestBecomesCommand() {
        assertThat(new RefreshRequest("eyJ.refresh").toCommand().refreshToken()).isEqualTo("eyJ.refresh");
    }

    @Test
    @DisplayName("Sessão com par expõe os dois tokens, seus jti e o id da sessão")
    void responseCarriesPair() {
        final var access = token(TokenUse.ACCESS, "eyJ.access", 3600L);
        final var refresh = token(TokenUse.REFRESH, "eyJ.refresh", 604800L);

        final var response = TokenResponse.from(new IssuedSession(SESSION_ID, access, refresh));

        assertThat(response.token()).isEqualTo("eyJ.access");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.id()).isEqualTo(access.id());
        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.refreshToken()).isEqualTo("eyJ.refresh");
        assertThat(response.refreshId()).isEqualTo(refresh.id());
        assertThat(response.refreshExpiresIn()).isEqualTo(604800L);
    }

    @Test
    @DisplayName("Sessão só com access deixa os campos de refresh nulos")
    void responseCarriesAccessOnly() {
        final var access = token(TokenUse.ACCESS, "eyJ.access", 3600L);

        final var response = TokenResponse.from(new IssuedSession(SESSION_ID, access, null));

        assertThat(response.refreshToken()).isNull();
        assertThat(response.refreshId()).isNull();
        assertThat(response.refreshExpiresIn()).isNull();
    }

    @Test
    @DisplayName("JSON da sessão só com access omite os campos de refresh em vez de mandar null")
    void serializesAccessOnlyWithoutRefreshFields() {
        final var response = TokenResponse.from(
                new IssuedSession(SESSION_ID, token(TokenUse.ACCESS, "eyJ.access", 3600L), null));

        final var json = mapper.readTree(mapper.writeValueAsString(response));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder("token", "type", "expiresIn", "id", "sessionId");
    }

    @Test
    @DisplayName("JSON da sessão com par traz todos os campos")
    void serializesPairWithRefreshFields() {
        final var response = TokenResponse.from(new IssuedSession(
                SESSION_ID,
                token(TokenUse.ACCESS, "eyJ.access", 3600L),
                token(TokenUse.REFRESH, "eyJ.refresh", 604800L)));

        final var json = mapper.readTree(mapper.writeValueAsString(response));

        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "token", "type", "expiresIn", "id", "sessionId", "refreshToken", "refreshId", "refreshExpiresIn");
    }

    private static IssuedToken token(final TokenUse use, final String value, final long expiresInSeconds) {
        return new IssuedToken(
                TokenClaims.create(use, NOW, NOW.plusSeconds(expiresInSeconds)),
                value,
                expiresInSeconds);
    }

}
