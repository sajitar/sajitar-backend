package com.sajitar.backend.adapter.in.web.contract.term;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import jakarta.validation.Valid;

@Tag(name = "Terms", description = "Operações de criação, publicação e consulta das linhas de termos de uso.")
@RequestMapping(value = Routes.TERM, produces = { APPLICATION_JSON_VALUE })
public interface TermApi {

    @Operation(
            summary = "Criar a primeira linha",
            description = """
                    Grava uma linha nova com um id gerado pelo servidor e draft true. \
                    Só o perfil cujo type inclui MASTER grava. O id novo não entra no vigente.""")
    @ApiResponse(
            responseCode = "200",
            description = "Linha criada com sucesso",
            content = @Content(schema = @Schema(implementation = TermResponse.class)))
    @SecurityRequirement(name = "bearer-jwt")
    @TermWriteErrorResponses
    @PostMapping
    ResponseEntity<TermResponse> postTerm(
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Valid @RequestBody CreateTermRequest request);

    @Operation(
            summary = "Acrescentar uma língua",
            description = """
                    Grava outra linha em um id existente, com draft true. Só o perfil MASTER grava. \
                    O id ausente responde 404. O par id e type já gravado responde 409 sem corpo.""")
    @ApiResponse(
            responseCode = "200",
            description = "Linha criada com sucesso",
            content = @Content(schema = @Schema(implementation = TermResponse.class)))
    @ApiResponse(responseCode = "404", description = "Id informado na URL não existe")
    @ApiResponse(responseCode = "409", description = "Par id e type já gravado")
    @SecurityRequirement(name = "bearer-jwt")
    @TermWriteErrorResponses
    @PostMapping("/{id}")
    ResponseEntity<TermResponse> postTermType(
            @Parameter(description = "Identificador do Term", example = "019c4000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @Valid @RequestBody CreateTermRequest request);

    @Operation(
            summary = "Alterar uma linha",
            description = """
                    Altera a linha da query type (EN, PT ou ES). A query ausente ou inválida responde 400 type. \
                    O corpo traz content, draft ou os dois. content ausente mantém o texto. draft ausente mantém o valor gravado. \
                    content nulo, vazio ou em branco responde 400 content. draft null responde 400 draft. \
                    Corpo sem content e sem draft responde 400 content e draft. Um type no corpo é ignorado. \
                    draft false publica a linha se o id tem EN, PT e ES; senão 400 draft. \
                    O id com EN, PT e ES publicados responde 400 draft antes do corpo. \
                    Uma linha publicada ainda aceita o patch enquanto outra língua é rascunho. Só o perfil MASTER grava.""")
    @ApiResponse(
            responseCode = "200",
            description = "Linha alterada com sucesso",
            content = @Content(schema = @Schema(implementation = TermResponse.class)))
    @ApiResponse(responseCode = "404", description = "Id informado na URL não existe")
    @SecurityRequirement(name = "bearer-jwt")
    @TermWriteErrorResponses
    @PatchMapping("/{id}")
    ResponseEntity<TermResponse> patchTerm(
            @Parameter(description = "Identificador do Term", example = "019c4000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Parameter(description = "Língua da linha: EN, PT ou ES", required = true, example = "EN")
            @RequestParam(required = false) String type,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session,
            @RequestBody PatchTermRequest request);

    @Operation(
            summary = "Apagar um Term",
            description = "Remove todas as linhas do id. Só o perfil MASTER apaga. O id ausente responde 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Term apagado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil autenticado não inclui MASTER"),
            @ApiResponse(responseCode = "404", description = "Id informado na URL não existe"),
            @ApiResponse(responseCode = "503", description = "Store de sessões indisponível")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteTerm(
            @Parameter(description = "Identificador do Term", example = "019c4000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Ler um Term por id",
            description = """
                    Retorna as linhas de um id para o perfil MASTER. Sem a query type, devolve id e contents com draft. \
                    Com type (EN, PT ou ES), devolve id, type, content e draft da linha pedida. \
                    O type pedido que o id não tem responde 400 type. A query type inválida responde 400 type.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Term encontrado",
                    content = @Content(schema = @Schema(implementation = TermResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Query type inválida, type pedido ausente ou id na URL não é UUID",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil autenticado não inclui MASTER"),
            @ApiResponse(responseCode = "404", description = "Id informado na URL não existe"),
            @ApiResponse(responseCode = "503", description = "Store de sessões indisponível")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/{id}")
    ResponseEntity<TermResponse> getTerm(
            @Parameter(description = "Identificador do Term", example = "019c4000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Parameter(description = "Língua pedida: EN, PT ou ES; omitida pede todas as linhas", example = "EN")
            @RequestParam(required = false) String type,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Listar os Term",
            description = """
                    Lista ids com paginação por cursor (lastSeenId, limit, reverse). Só o perfil MASTER lista. \
                    Sem a query type, cada item traz id e contents com draft. Com type (EN, PT ou ES), cada item traz \
                    id, type, content e draft, e a página inclui só os ids que têm o type. A página conta ids, não linhas.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página (content, precedingElements, followingElements, reverse)",
                    content = @Content(schema = @Schema(implementation = TermPageResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Query type inválida ou parâmetros de cursor inválidos",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bearer ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil autenticado não inclui MASTER"),
            @ApiResponse(responseCode = "404", description = "Nenhum resultado para os critérios informados"),
            @ApiResponse(responseCode = "503", description = "Store de sessões indisponível")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping
    ResponseEntity<TermPageResponse> getTerms(
            @Parameter(description = "Língua pedida: EN, PT ou ES; omitida pede todas as linhas de cada id", example = "EN")
            @RequestParam(required = false) String type,
            @Parameter(description = "Cursor: último id visto na listagem")
            @RequestParam(required = false) UUID lastSeenId,
            @Parameter(description = "Tamanho máximo da página (1–100)", example = "100")
            @RequestParam(defaultValue = "100", required = false) int limit,
            @Parameter(description = "Ordenação descendente quando true", example = "false")
            @RequestParam(defaultValue = "false", required = false) boolean reverse,
            @Parameter(hidden = true) @AuthenticationPrincipal Session session);

    @Operation(
            summary = "Ler o Term vigente",
            description = """
                    Retorna o maior id com EN, PT e ES publicados. Rota pública, sem Bearer. O corpo omite draft. \
                    Sem a query lang, devolve id e contents. Com lang, devolve id, type e content. \
                    Sem vigente, responde 404. A query lang inválida responde 400 lang.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Term vigente encontrado",
                    content = @Content(schema = @Schema(implementation = TermResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Query lang inválida",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Não há id com as três línguas publicadas")
    })
    @GetMapping("/current")
    ResponseEntity<TermResponse> getCurrentTerm(
            @Parameter(description = "Língua pedida: en, pt ou es; omitida pede todas as linhas", example = "en")
            @RequestParam(required = false) String lang);

}
