package com.sajitar.backend.adapter.in.web.contract.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.token.ActiveSession;
import com.sajitar.backend.domain.model.token.Client;
import com.sajitar.backend.domain.model.token.IssuedSession;
import com.sajitar.backend.domain.model.token.IssuedToken;
import com.sajitar.backend.domain.model.token.TokenClaims;
import com.sajitar.backend.domain.model.token.TokenUse;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("Contratos de /tokens")
class TokenContractTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final UUID SESSION_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000010");

    private static final String ADDRESS = "203.0.113.10";

    private static final Client CLIENT = new Client("Chrome", "Linux", Client.Device.DESKTOP);

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("SignInRequest converte credenciais e o opt-in de refresh para o command")
    void signInRequestBecomesCommand() {
        final var command = new SignInRequest("alice@example.com", "senhaSegura1", true)
                .toCommand(ADDRESS, CLIENT);

        assertThat(command.email()).isEqualTo("alice@example.com");
        assertThat(command.password()).isEqualTo("senhaSegura1");
        assertThat(command.refresh()).isTrue();
        assertThat(command.address()).isEqualTo(ADDRESS);
        assertThat(command.client()).isEqualTo(CLIENT);
    }

    @Test
    @DisplayName("SignInRequest sem refresh mantém o padrão de sessão só com access")
    void signInRequestDefaultsToAccessOnly() {
        assertThat(new SignInRequest("alice@example.com", "senhaSegura1", false)
                .toCommand(ADDRESS, null)
                .refresh()).isFalse();
    }

    @Test
    @DisplayName("RefreshRequest converte o token para o command")
    void refreshRequestBecomesCommand() {
        final var command = new RefreshRequest("eyJ.refresh").toCommand(ADDRESS, CLIENT);

        assertThat(command.refreshToken()).isEqualTo("eyJ.refresh");
        assertThat(command.address()).isEqualTo(ADDRESS);
        assertThat(command.client()).isEqualTo(CLIENT);
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

    @Test
    @DisplayName("SignOutRequest leva o perfil e a sessão corrente do Bearer para o command")
    void signOutRequestBecomesCommand() {
        final var profileId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        final var command = new SignOutRequest(List.of(SESSION_ID), "senhaSegura1")
                .toCommand(profileId, SESSION_ID, ADDRESS);

        assertThat(command.profileId()).isEqualTo(profileId);
        assertThat(command.currentSessionId()).isEqualTo(SESSION_ID);
        assertThat(command.ids()).containsExactly(SESSION_ID);
        assertThat(command.password()).isEqualTo("senhaSegura1");
        assertThat(command.address()).isEqualTo(ADDRESS);
        assertThat(command.requiresPassword()).isFalse();
    }

    @Test
    @DisplayName("SignOutRequest com outra sessão exige senha no command")
    void signOutRequestOfAnotherSessionRequiresPassword() {
        final var other = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000020");

        final var command = new SignOutRequest(List.of(SESSION_ID, other), null)
                .toCommand(UUID.randomUUID(), SESSION_ID, ADDRESS);

        assertThat(command.requiresPassword()).isTrue();
    }

    @Test
    @DisplayName("Listagem marca como corrente só a sessão do Bearer")
    void sessionsResponseMarksCurrent() {
        final var other = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000020");

        final var response = SessionsResponse.from(List.of(
                new ActiveSession(SESSION_ID, CLIENT),
                new ActiveSession(other, null)), SESSION_ID);

        assertThat(response.content()).containsExactly(
                new SessionResponse(SESSION_ID, true, ClientResponse.from(CLIENT)),
                new SessionResponse(other, false, null));
    }

    @Test
    @DisplayName("JSON da listagem traz só content, com id e current por sessão")
    void serializesSessionsWithoutExtraFields() {
        final var response = SessionsResponse.from(List.of(new ActiveSession(SESSION_ID, null)), SESSION_ID);

        final var json = mapper.readTree(mapper.writeValueAsString(response));

        assertThat(json.propertyNames()).containsExactly("content");
        assertThat(json.get("content").get(0).propertyNames()).containsExactlyInAnyOrder("id", "current");
    }

    @Test
    @DisplayName("Perfil sem sessão serializa content vazio, não null")
    void serializesEmptySessions() {
        final var json = mapper.readTree(mapper.writeValueAsString(SessionsResponse.from(List.of(), SESSION_ID)));

        assertThat(json.get("content").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("JSON da listagem inclui client quando a sessão tem User-Agent parseado")
    void serializesClientWhenPresent() {
        final var response = SessionsResponse.from(List.of(new ActiveSession(SESSION_ID, CLIENT)), SESSION_ID);

        final var json = mapper.readTree(mapper.writeValueAsString(response));
        final var item = json.get("content").get(0);
        final var client = item.get("client");

        assertThat(item.propertyNames()).containsExactlyInAnyOrder("id", "current", "client");
        assertThat(client.propertyNames()).containsExactlyInAnyOrder("name", "os", "device");
        assertThat(client.get("name").asText()).isEqualTo("Chrome");
        assertThat(client.get("os").asText()).isEqualTo("Linux");
        assertThat(client.get("device").asText()).isEqualTo("desktop");
    }

    @Test
    @DisplayName("JSON do client omite os e device nulos")
    void serializesClientOmittingNullFields() {
        final var client = new Client("Chrome", null, null);
        final var response = SessionsResponse.from(List.of(new ActiveSession(SESSION_ID, client)), SESSION_ID);

        final var json = mapper.readTree(mapper.writeValueAsString(response)).get("content").get(0).get("client");

        assertThat(json.propertyNames()).containsExactly("name");
        assertThat(ClientResponse.from(null)).isNull();
    }

    private static IssuedToken token(final TokenUse use, final String value, final long expiresInSeconds) {
        return new IssuedToken(
                TokenClaims.create(use, NOW, NOW.plusSeconds(expiresInSeconds)),
                value,
                expiresInSeconds);
    }

}
