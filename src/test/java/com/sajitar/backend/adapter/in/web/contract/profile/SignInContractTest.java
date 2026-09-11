package com.sajitar.backend.adapter.in.web.contract.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.command.profile.RefreshProfileCommand;
import com.sajitar.backend.application.command.profile.SignInProfileCommand;
import com.sajitar.backend.domain.port.AccessToken;
import com.sajitar.backend.domain.port.TokenPair;

@DisplayName("SignInRequest, RefreshRequest e SignInResponse")
class SignInContractTest {

    @Test
    @DisplayName("toCommand copia e-mail e senha")
    void requestToCommandCopiesCredentials() {
        final var request = new SignInRequest("alice@example.com", "senhaSegura1");

        assertThat(request.toCommand()).isEqualTo(new SignInProfileCommand("alice@example.com", "senhaSegura1"));
    }

    @Test
    @DisplayName("toCommand copia o refresh token")
    void refreshRequestToCommandCopiesToken() {
        final var request = new RefreshRequest("refresh.jwt.value");

        assertThat(request.toCommand()).isEqualTo(new RefreshProfileCommand("refresh.jwt.value"));
    }

    @Test
    @DisplayName("from copia o par JWT e fixa type Bearer")
    void responseFromCopiesTokenPair() {
        final var pair = new TokenPair(
                new AccessToken("jwt.access.value", 3600),
                new AccessToken("jwt.refresh.value", 604800));

        final var response = SignInResponse.from(pair);

        assertThat(response.token()).isEqualTo("jwt.access.value");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.refreshToken()).isEqualTo("jwt.refresh.value");
        assertThat(response.refreshExpiresIn()).isEqualTo(604800);
    }

}
