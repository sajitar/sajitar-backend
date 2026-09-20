package com.sajitar.backend.adapter.in.web.contract.token;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.adapter.in.web.contract.ValidationErrorResponse;
import com.sajitar.backend.domain.model.token.Session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
                    Perfil com checker VERIFY_EMAIL recebe 403. Limite de tentativas por endereço e e-mail responde 429. \
                    Endpoint público: o header Authorization é ignorado.""")
    @ApiResponse(
            responseCode = "200",
            description = "Sessão aberta",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @SignInErrorResponses
    @PostMapping("/signin")
    ResponseEntity<TokenResponse> postSignIn(
            @Valid @RequestBody SignInRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Rotacionar tokens",
            description = """
                    Troca o refresh vigente por um par novo na mesma sessão: o refresh apresentado e o access ligado a ele \
                    deixam de valer. Um retry dentro da janela de graça devolve o mesmo par sucessor; fora dela, o reuso \
                    encerra a sessão e responde 401. Limite de tentativas por endereço responde 429. \
                    Access JWT não é aceito aqui, nem no header Authorization, que é ignorado.""")
    @ApiResponse(
            responseCode = "200",
            description = "Par novo emitido",
            content = @Content(schema = @Schema(implementation = TokenResponse.class)))
    @RefreshErrorResponses
    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> postRefresh(
            @Valid @RequestBody RefreshRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Listar sessões",
            description = """
                    Lista as sessões de login ativas do perfil autenticado, da mais antiga para a mais recente. \
                    O instante do login está nos 48 bits de tempo do id de cada sessão, e `current` marca a sessão \
                    do Bearer desta requisição. `client` é o User-Agent parseado (omitido se o parse falhar). \
                    Não devolve o JWT nem os jti; lista sem item não é 404.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sessões ativas do perfil",
                    content = @Content(schema = @Schema(implementation = SessionsResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Bearer ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Store de sessões indisponível")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping
    ResponseEntity<SessionsResponse> getSessions(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Encerrar sessões",
            description = """
                    Encerra em lote as sessões do próprio perfil, removendo o access e o refresh de cada uma. \
                    O Bearer basta quando `ids` traz apenas a sessão corrente; qualquer outra sessão exige `password`. \
                    Tentativas com senha entram no mesmo limite do signin (429). \
                    O lote é tudo ou nada: 204 se todos os ids forem sessões ativas suas, 404 sem encerrar nada caso contrário.""")
    @ApiResponse(responseCode = "204", description = "Sessões encerradas")
    @SecurityRequirement(name = "bearer-jwt")
    @SignOutErrorResponses
    @PostMapping("/signout")
    ResponseEntity<Void> postSignOut(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @RequestBody SignOutRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

}
