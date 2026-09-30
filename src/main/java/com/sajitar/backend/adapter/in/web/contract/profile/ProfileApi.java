package com.sajitar.backend.adapter.in.web.contract.profile;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

@Tag(name = "Profiles", description = "Operações de criação, atualização, exclusão e consulta de perfis.")
@RequestMapping(value = Routes.PROFILE, produces = { APPLICATION_JSON_VALUE })
public interface ProfileApi {

    @Operation(
            summary = "Criar perfil",
            description = """
                    Cria um novo perfil. O identificador é gerado pelo servidor e não deve ser enviado no corpo. \
                    O type omitido ou nulo grava WRITER. Só um access Bearer cujo perfil inclui MASTER grava o \
                    type enviado; sem Bearer, Bearer inválido ou caller que não é MASTER, o type do corpo é \
                    ignorado. Bearer inválido não gera 401. \
                    O sistema cria internamente um checker VERIFY_EMAIL e envia o código de verificação de seis \
                    dígitos ao e-mail informado. Enquanto o checker existir, signin e refresh respondem 403; \
                    o reenvio do código é POST /tokens/verification.""")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil criado com sucesso",
            content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class)))
    @ApiResponse(responseCode = "503", description = "Serviço de correio indisponível, ou store de sessões indisponível se o header Authorization Bearer estiver presente")
    @ProfileWriteErrorResponses
    @PostMapping
    ResponseEntity<ProfileSummaryResponse> postProfile(
            @Valid @RequestBody CreateProfileRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Trocar a própria senha",
            description = """
                    Confere a senha atual do perfil autenticado e grava a nova. Com signoutAllSessions true, \
                    encerra todas as sessões daquele perfil antes da escrita; omitido ou false mantém as sessões. \
                    O identificador sai da sessão, não do corpo. PUT e PATCH não trocam senha.""")
    @ApiResponse(responseCode = "204", description = "Senha alterada")
    @SecurityRequirement(name = "bearer-jwt")
    @ChangeOwnPasswordErrorResponses
    @PostMapping("/password")
    ResponseEntity<Void> postPassword(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Valid @RequestBody ChangeOwnPasswordRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Pedir código de recuperação de senha",
            description = """
                    Cria ou gira o checker CHANGE_PASSWORD e envia o código de seis dígitos ao e-mail. \
                    Sempre responde 204: e-mail desconhecido, perfil ainda com VERIFY_EMAIL ou checker com mais \
                    de 30 minutos não enviam correio e não revelam o caso. Um reenvio dentro do prazo gera código \
                    novo (o anterior deixa de valer) sem reabrir os 30 minutos. O código não volta no JSON. \
                    Limite de tentativas por endereço e e-mail (o mesmo do signin) responde 429. \
                    Endpoint público: o header Authorization é ignorado.""")
    @ApiResponse(responseCode = "204", description = "Pedido aceito")
    @RecoverPasswordErrorResponses
    @PostMapping("/password/recovery")
    ResponseEntity<Void> postPasswordRecovery(
            @Valid @RequestBody RecoverPasswordRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Confirmar recuperação de senha",
            description = """
                    Confere o código de CHANGE_PASSWORD e grava a senha nova. Encerra todas as sessões daquele \
                    perfil antes da escrita. Código ausente ou mal formado responde 400; e-mail inexistente, \
                    checker ausente ou vencido, ou código divergente respondem 401 (o código vigente não muda). \
                    Limite de tentativas por endereço e e-mail responde 429. \
                    Endpoint público: o header Authorization é ignorado.""")
    @ApiResponse(responseCode = "204", description = "Senha alterada")
    @ConfirmPasswordRecoveryErrorResponses
    @PostMapping("/password/confirm")
    ResponseEntity<Void> postPasswordConfirm(
            @Valid @RequestBody ConfirmPasswordRecoveryRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Pedir código de troca de e-mail",
            description = """
                    Cria ou gira o checker CHANGE_EMAIL do perfil autenticado e envia o código de seis dígitos \
                    ao e-mail vigente. Sempre recomeça a troca: limpa o payload, se houver, e gira o código \
                    sem reabrir os 30 minutos, para o confirm aceitar um newEmail novo. Corpo vazio. \
                    O código não volta no JSON. Perfil ainda com VERIFY_EMAIL responde 403; checker vencido \
                    responde 401. Limite de tentativas por endereço e e-mail vigente responde 429.""")
    @ApiResponse(responseCode = "204", description = "Pedido aceito")
    @SecurityRequirement(name = "bearer-jwt")
    @RequestChangeEmailErrorResponses
    @PostMapping("/email/recovery")
    ResponseEntity<Void> postEmailRecovery(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Confirmar o e-mail vigente e informar o novo",
            description = """
                    Confere o código enviado ao e-mail vigente, grava newEmail no payload e envia um código novo \
                    só para esse endereço. O e-mail do perfil ainda não muda. Código ausente ou mal formado \
                    responde 400; newEmail igual ao vigente responde 400; checker ausente, vencido, payload já \
                    preenchido ou código divergente respondem 401 (o código vigente não muda); e-mail de outro \
                    perfil responde 409 sem consumir o código.""")
    @ApiResponse(responseCode = "204", description = "Payload gravado")
    @SecurityRequirement(name = "bearer-jwt")
    @ConfirmChangeEmailErrorResponses
    @PostMapping("/email/confirm")
    ResponseEntity<Void> postEmailConfirm(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Valid @RequestBody ConfirmChangeEmailRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Concluir a troca de e-mail",
            description = """
                    Confere o código enviado ao e-mail novo, encerra todas as sessões daquele perfil antes da \
                    escrita, grava o payload no perfil e apaga o checker. O código da primeira etapa, sozinho, \
                    não conclui a troca. Código ausente ou mal formado responde 400; payload nulo, checker \
                    ausente ou vencido, ou código divergente respondem 401; e-mail de outro perfil responde 409 \
                    com o checker preservado.""")
    @ApiResponse(responseCode = "204", description = "E-mail alterado")
    @SecurityRequirement(name = "bearer-jwt")
    @ChangeEmailErrorResponses
    @PostMapping("/email/change")
    ResponseEntity<Void> postEmailChange(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Valid @RequestBody ChangeEmailRequest request,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Pedir código de exclusão do próprio perfil",
            description = """
                    Cria ou gira o checker DELETE_PROFILE do perfil autenticado e envia o código de seis dígitos \
                    ao e-mail vigente. Corpo vazio. O código não volta no JSON. Um reenvio dentro do prazo gera \
                    código novo sem reabrir os 30 minutos. Perfil ainda com VERIFY_EMAIL responde 403; checker \
                    vencido responde 401. Limite de tentativas por endereço e e-mail vigente responde 429.""")
    @ApiResponse(responseCode = "204", description = "Pedido aceito")
    @SecurityRequirement(name = "bearer-jwt")
    @RequestProfileDeletionErrorResponses
    @PostMapping("/deletion")
    ResponseEntity<Void> postProfileDeletion(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Atualizar perfil",
            description = """
                    Substitui um perfil existente. O identificador vem exclusivamente da URL e não pode ser alterado. \
                    name, description e birthday só o próprio perfil grava; caller alheio, inclusive MASTER, recebe 403 {id}. \
                    O type (MASTER, WRITER ou READER) é obrigatório. Só um caller MASTER substitui o vigente por \
                    um valor diferente, no próprio perfil ou em outro quando os demais campos efetivos são iguais; \
                    o mesmo type segue 200. \
                    A senha não é aceita neste recurso; use POST /profiles/password. \
                    O e-mail não é aceito neste recurso; use POST /profiles/email/recovery.""")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class)))
    @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido")
    @ApiResponse(
            responseCode = "403",
            description = "Caller alheio tentou alterar name, description ou birthday, ou caller sem tipo MASTER tentou alterar o type",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    @SecurityRequirement(name = "bearer-jwt")
    @ProfileWriteErrorResponses
    @PutMapping("/{id}")
    ResponseEntity<ProfileSummaryResponse> putProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Atualizar perfil parcialmente",
            description = """
                    Atualiza apenas os campos enviados no corpo. Campos omitidos permanecem inalterados. \
                    O identificador vem exclusivamente da URL e não pode ser alterado. \
                    name, description e birthday só o próprio perfil grava; caller alheio, inclusive MASTER, recebe 403 {id}. \
                    type omitido ou nulo mantém o vigente. Só um caller MASTER substitui o vigente por um valor \
                    diferente, no próprio perfil ou em outro quando os demais campos efetivos são iguais. \
                    Descrição nula remove o valor atual. A senha não é aceita neste recurso; use POST /profiles/password. \
                    O e-mail não é aceito neste recurso; use POST /profiles/email/recovery.""")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class)))
    @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido")
    @ApiResponse(
            responseCode = "403",
            description = "Caller alheio tentou alterar name, description ou birthday, ou caller sem tipo MASTER tentou alterar o type",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    @SecurityRequirement(name = "bearer-jwt")
    @ProfileWriteErrorResponses
    @PatchMapping("/{id}")
    ResponseEntity<ProfileSummaryResponse> patchProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody PatchProfileRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Excluir o próprio perfil",
            description = """
                    Remove o perfil da sessão. O identificador da URL tem de ser o do Bearer, inclusive se o \
                    caller for MASTER. Confere o código de DELETE_PROFILE enviado ao e-mail vigente, encerra \
                    todas as sessões antes da exclusão e apaga o perfil. Os checkers saem no CASCADE. \
                    A exclusão não é idempotente.""")
    @ApiResponse(responseCode = "204", description = "Perfil excluído com sucesso")
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteProfileErrorResponses
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody DeleteProfileRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
            summary = "Obter perfil por id",
            description = """
                    Retorna a visão resumida (id, type, nome e descrição) de um perfil. \
                    Perfil com checker VERIFY_EMAIL é 404 para quem não tem tipo MASTER. \
                    Perfil READER alheio também é 404; o próprio READER vê o resumo.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil encontrado",
                    content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/{id}")
    ResponseEntity<ProfileSummaryResponse> getProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Obter detalhes do perfil",
            description = """
                    Retorna os detalhes completos de um perfil, incluindo e-mail e data de nascimento. \
                    Só o dono ou um caller MASTER vê os detalhes. Quem não é MASTER e pede outro perfil \
                    recebe 403.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil encontrado",
                    content = @Content(schema = @Schema(implementation = ProfileDetailsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller sem tipo MASTER pediu detalhes de outro perfil",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/{id}/details")
    ResponseEntity<ProfileDetailsResponse> getProfileDetails(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Listar perfis",
            description = """
                    Lista perfis com paginação por cursor. Sem parâmetro `name`, lista todos os perfis visíveis; \
                    com `name`, filtra por substring no nome (case-insensitive). \
                    Cursor completo (`lastSeenName` + `lastSeenId`) avança a página. \
                    O perfil da sessão não entra em content nem nas contagens. \
                    Caller MASTER pode filtrar por `type` (`MASTER`, `WRITER` ou `READER`); omitido lista todos \
                    os tipos visíveis. Quem não é MASTER e envia `type` recebe 403. \
                    Caller MASTER pode filtrar por `verified` (true = sem checker VERIFY_EMAIL, false = só com \
                    o checker); omitido lista os dois conjuntos. Quem não é MASTER e envia `verified` recebe 403. \
                    Quem não tem tipo MASTER não vê perfis com checker VERIFY_EMAIL nem READER alheio. \
                    O GET por id do próprio perfil permanece.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = ProfilePageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos"),
            @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Caller sem tipo MASTER enviou o query param type ou verified",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nenhum resultado para os critérios informados")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping
    ResponseEntity<ProfilePageResponse> getProfiles(
            @Parameter(description = "Substring para busca no nome (opcional)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filtro de tipo; só MASTER. Omitido lista todos os tipos visíveis")
            @RequestParam(required = false) String type,
            @Parameter(description = "Filtro de verificação de e-mail; só MASTER. true = sem VERIFY_EMAIL, false = só com o checker; omitido lista ambos")
            @RequestParam(required = false) Boolean verified,
            @Parameter(description = "Nome do último item visto (cursor)")
            @RequestParam(required = false) String lastSeenName,
            @Parameter(description = "Id do último item visto (cursor)")
            @RequestParam(required = false) UUID lastSeenId,
            @Parameter(description = "Tamanho máximo da página (1–100)", example = "100")
            @RequestParam(defaultValue = "100", required = false) int limit,
            @Parameter(description = "Ordenação descendente quando true", example = "false")
            @RequestParam(defaultValue = "false", required = false) boolean reverse,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

}
