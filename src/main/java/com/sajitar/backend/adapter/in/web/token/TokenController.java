package com.sajitar.backend.adapter.in.web.token;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.token.RefreshRequest;
import com.sajitar.backend.adapter.in.web.contract.token.SessionsResponse;
import com.sajitar.backend.adapter.in.web.contract.token.SignInRequest;
import com.sajitar.backend.adapter.in.web.contract.token.SignOutRequest;
import com.sajitar.backend.adapter.in.web.contract.token.TokenApi;
import com.sajitar.backend.adapter.in.web.contract.token.TokenResponse;
import com.sajitar.backend.application.query.token.ListSessionsQuery;
import com.sajitar.backend.application.usecase.token.ListSessionsUseCase;
import com.sajitar.backend.application.usecase.token.RefreshTokenUseCase;
import com.sajitar.backend.application.usecase.token.SignInTokenUseCase;
import com.sajitar.backend.application.usecase.token.SignOutTokenUseCase;
import com.sajitar.backend.domain.model.token.Session;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TokenController implements TokenApi {

    private final SignInTokenUseCase signInToken;

    private final RefreshTokenUseCase refreshToken;

    private final ListSessionsUseCase listSessions;

    private final SignOutTokenUseCase signOutToken;

    @Override
    public ResponseEntity<TokenResponse> postSignIn(final SignInRequest request) {
        return ResponseEntity.ok(TokenResponse.from(signInToken.execute(request.toCommand())));
    }

    @Override
    public ResponseEntity<TokenResponse> postRefresh(final RefreshRequest request) {
        return ResponseEntity.ok(TokenResponse.from(refreshToken.execute(request.toCommand())));
    }

    @Override
    public ResponseEntity<SessionsResponse> getSessions(final Session session) {
        final var ids = listSessions.execute(new ListSessionsQuery(session.profileId()));
        return ResponseEntity.ok(SessionsResponse.from(ids, session.id()));
    }

    @Override
    public ResponseEntity<Void> postSignOut(final Session session, final SignOutRequest request) {
        signOutToken.execute(request.toCommand(session.profileId(), session.id()));
        return ResponseEntity.noContent().build();
    }

}
