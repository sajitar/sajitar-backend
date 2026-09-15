package com.sajitar.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.port.token.AccessTokenDecoder;
import com.sajitar.backend.domain.port.token.SessionStore;

@ExtendWith(MockitoExtension.class)
@DisplayName("BearerSessionAuthenticationFilter")
class BearerSessionAuthenticationFilterTest {

    private static final UUID ACCESS_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000001");

    private static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private AccessTokenDecoder accessTokens;

    @Mock
    private SessionStore sessions;

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Access com registro ativo autentica o perfil lido do store")
    void authenticatesProfileFromStore() throws Exception {
        when(accessTokens.accessId("eyJ.access")).thenReturn(Optional.of(ACCESS_ID));
        when(sessions.profileIdOfActiveAccess(ACCESS_ID)).thenReturn(Optional.of(PROFILE_ID));

        filter().doFilter(request("Bearer eyJ.access"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(PROFILE_ID);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Sem header não autentica e deixa o entry point responder")
    void skipsWhenHeaderIsMissing() throws Exception {
        filter().doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verify(accessTokens, never()).accessId(any());
    }

    @Test
    @DisplayName("Authorization que não é Bearer é ignorado")
    void skipsWhenSchemeIsNotBearer() throws Exception {
        filter().doFilter(request("Basic dXNlcjpwYXNz"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(accessTokens, never()).accessId(any());
    }

    @Test
    @DisplayName("Access que não decodifica segue sem autenticação")
    void skipsWhenTokenIsInvalid() throws Exception {
        when(accessTokens.accessId("not-a-jwt")).thenReturn(Optional.empty());

        filter().doFilter(request("Bearer not-a-jwt"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessions, never()).profileIdOfActiveAccess(any());
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Access sem registro ativo segue sem autenticação")
    void skipsWhenSessionIsGone() throws Exception {
        when(accessTokens.accessId("eyJ.access")).thenReturn(Optional.of(ACCESS_ID));
        when(sessions.profileIdOfActiveAccess(ACCESS_ID)).thenReturn(Optional.empty());

        filter().doFilter(request("Bearer eyJ.access"), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("Store indisponível responde 503 e interrompe a cadeia")
    void failsClosedWhenStoreIsUnavailable() throws Exception {
        when(accessTokens.accessId("eyJ.access")).thenReturn(Optional.of(ACCESS_ID));
        when(sessions.profileIdOfActiveAccess(ACCESS_ID)).thenThrow(new SessionStoreUnavailableException());

        filter().doFilter(request("Bearer eyJ.access"), response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
    }

    private BearerSessionAuthenticationFilter filter() {
        return new BearerSessionAuthenticationFilter(accessTokens, sessions);
    }

    private static MockHttpServletRequest request(final String authorization) {
        final var request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }

}
