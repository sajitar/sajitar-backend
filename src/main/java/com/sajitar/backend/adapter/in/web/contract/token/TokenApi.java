package com.sajitar.backend.adapter.in.web.contract.token;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sajitar.backend.adapter.in.web.Routes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Tokens", description = "Abertura de sessão e rotação de tokens JWT.")
@RequestMapping(value = Routes.TOKEN, produces = { APPLICATION_JSON_VALUE })
public interface TokenApi {

    @Operation(
            summary = "Abrir sessão",
            description = """
                    Valida e-mail e senha, cria uma sessão de login e devolve um access token. \
                    O refresh só é emitido quando o corpo traz `refresh: true`. \
                    Se o perfil já estiver no teto de sessões ativas, a mais antiga é encerrada. \
                    Perfil com checker VERIFY_EMAIL recebe 403. Endpoint público: o header Authorization é ignorado.""")
    @ApiResponse(
            responseCode = "200",
            description = "Sessão aberta",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @SignInErrorResponses
    @PostMapping("/signin")
    ResponseEntity<TokenResponse> postSignIn(@Valid @RequestBody SignInRequest request);

    @Operation(
            summary = "Rotacionar tokens",
            description = """
                    Troca o refresh vigente por um par novo na mesma sessão: o refresh apresentado e o access ligado a ele \
                    deixam de valer. Um retry dentro da janela de graça devolve o mesmo par sucessor; fora dela, o reuso \
                    encerra a sessão e responde 401. Access JWT não é aceito aqui, nem no header Authorization, que é ignorado.""")
    @ApiResponse(
            responseCode = "200",
            description = "Par novo emitido",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @RefreshErrorResponses
    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> postRefresh(@Valid @RequestBody RefreshRequest request);

}
