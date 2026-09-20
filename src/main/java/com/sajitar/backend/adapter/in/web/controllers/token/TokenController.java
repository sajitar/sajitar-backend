package com.sajitar.backend.adapter.in.web.controllers.token;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contracts.token.RefreshRequest;
import com.sajitar.backend.adapter.in.web.contracts.token.SessionsResponse;
import com.sajitar.backend.adapter.in.web.contracts.token.SignInRequest;
import com.sajitar.backend.adapter.in.web.contracts.token.SignOutRequest;
import com.sajitar.backend.adapter.in.web.contracts.token.TokenApi;
import com.sajitar.backend.adapter.in.web.contracts.token.TokenResponse;
import com.sajitar.backend.application.query.token.ListSessionsQuery;
import com.sajitar.backend.application.usecase.token.ListSessionsUseCase;
import com.sajitar.backend.application.usecase.token.RefreshTokenUseCase;
import com.sajitar.backend.application.usecase.token.SignInTokenUseCase;
import com.sajitar.backend.application.usecase.token.SignOutTokenUseCase;
import com.sajitar.backend.domain.model.token.Session;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TokenController implements TokenApi {

    private final SignInTokenUseCase signInToken;

    private final RefreshTokenUseCase refreshToken;

    private final ListSessionsUseCase listSessions;

    private final SignOutTokenUseCase signOutToken;

    private final RequestOrigins origins;

    @Override
    public ResponseEntity<TokenResponse> postSignIn(final SignInRequest request, final HttpServletRequest http) {
        return ResponseEntity.ok(TokenResponse.from(
                signInToken.execute(request.toCommand(origins.address(http), origins.client(http)))));
    }

    @Override
    public ResponseEntity<TokenResponse> postRefresh(final RefreshRequest request, final HttpServletRequest http) {
        return ResponseEntity.ok(TokenResponse.from(
                refreshToken.execute(request.toCommand(origins.address(http), origins.client(http)))));
    }

    @Override
    public ResponseEntity<SessionsResponse> getSessions(final Session session) {
        final var sessions = listSessions.execute(new ListSessionsQuery(session.profileId()));
        return ResponseEntity.ok(SessionsResponse.from(sessions, session.id()));
    }

    @Override
    public ResponseEntity<Void> postSignOut(
            final Session session,
            final SignOutRequest request,
            final HttpServletRequest http) {
        signOutToken.execute(request.toCommand(session.profileId(), session.id(), origins.address(http)));
        return ResponseEntity.noContent().build();
    }

}
