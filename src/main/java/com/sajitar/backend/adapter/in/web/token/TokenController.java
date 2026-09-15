package com.sajitar.backend.adapter.in.web.token;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sajitar.backend.adapter.in.web.contract.token.RefreshRequest;
import com.sajitar.backend.adapter.in.web.contract.token.SignInRequest;
import com.sajitar.backend.adapter.in.web.contract.token.TokenApi;
import com.sajitar.backend.adapter.in.web.contract.token.TokenResponse;
import com.sajitar.backend.application.usecase.token.RefreshTokenUseCase;
import com.sajitar.backend.application.usecase.token.SignInTokenUseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class TokenController implements TokenApi {

    private final SignInTokenUseCase signInToken;

    private final RefreshTokenUseCase refreshToken;

    @Override
    public ResponseEntity<TokenResponse> postSignIn(final SignInRequest request) {
        return ResponseEntity.ok(TokenResponse.from(signInToken.execute(request.toCommand())));
    }

    @Override
    public ResponseEntity<TokenResponse> postRefresh(final RefreshRequest request) {
        return ResponseEntity.ok(TokenResponse.from(refreshToken.execute(request.toCommand())));
    }

}
