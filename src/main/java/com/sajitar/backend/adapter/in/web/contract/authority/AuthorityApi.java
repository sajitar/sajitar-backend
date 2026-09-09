package com.sajitar.backend.adapter.in.web.contract.authority;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Authorities", description = "Operações de criação, atualização, exclusão e consulta de papéis de um perfil.")
@RequestMapping(value = Routes.AUTHORITY, produces = { APPLICATION_JSON_VALUE })
public interface AuthorityApi {

    @Operation(
            summary = "Criar authority",
            description = "Cria uma authority para o perfil informado na query. Corpo: só type. Tipos públicos: MASTER, MEMBER e READER.")
    @ApiResponse(
            responseCode = "200",
            description = "Authority criada com sucesso",
            content = @Content(schema = @Schema(implementation = AuthorityResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Perfil informado na query não existe",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @AuthorityWriteErrorResponses
    @PostMapping
    ResponseEntity<AuthorityResponse> postAuthority(
            @Parameter(description = "Identificador do perfil dono da authority")
            @RequestParam(required = false) UUID profileId,
            @Valid @RequestBody CreateAuthorityRequest request);

    @Operation(
            summary = "Atualizar authority",
            description = """
                    Substitui type. Sem mudança real o servidor não grava. \
                    O identificador vem exclusivamente da URL.""")
    @ApiResponse(
            responseCode = "200",
            description = "Authority atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = AuthorityResponse.class)))
    @ApiResponse(responseCode = "404", description = "Authority não encontrada")
    @AuthorityWriteErrorResponses
    @PutMapping("/{id}")
    ResponseEntity<AuthorityResponse> putAuthority(
            @Parameter(description = "Identificador da authority", example = "019c2000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAuthorityRequest request);

    @Operation(
            summary = "Atualizar authority parcialmente",
            description = """
                    Atualiza type. Campo omitido ou nulo permanece inalterado. \
                    Sem mudança real o servidor não grava. \
                    O identificador vem exclusivamente da URL.""")
    @ApiResponse(
            responseCode = "200",
            description = "Authority atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = AuthorityResponse.class)))
    @ApiResponse(responseCode = "404", description = "Authority não encontrada")
    @AuthorityWriteErrorResponses
    @PatchMapping("/{id}")
    ResponseEntity<AuthorityResponse> patchAuthority(
            @Parameter(description = "Identificador da authority", example = "019c2000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Valid @RequestBody PatchAuthorityRequest request);

    @Operation(
            summary = "Excluir authority",
            description = "Remove a authority identificada pela URL. O identificador não pode ser alterado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Authority excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Authority não encontrada")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteAuthority(
            @Parameter(description = "Identificador da authority", example = "019c2000-a111-7000-8000-111111111111")
            @PathVariable UUID id);

    @Operation(summary = "Obter authority por id", description = "Retorna a authority completa.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authority encontrada",
                    content = @Content(schema = @Schema(implementation = AuthorityResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Authority não encontrada")
    })
    @GetMapping("/{id}")
    ResponseEntity<AuthorityResponse> getAuthority(
            @Parameter(description = "Identificador da authority", example = "019c2000-a111-7000-8000-111111111111")
            @PathVariable UUID id);

    @Operation(
            summary = "Consultar authorities do perfil",
            description = """
                    Com `type`, devolve uma única authority do par (profileId, type) e ignora \
                    `lastSeenType`, `limit` e `reverse`. Sem `type`, lista as authorities do perfil com paginação \
                    por cursor (`lastSeenType`, `limit`, `reverse`). \
                    A página JSON contém só `content`, `precedingElements`, `followingElements` e `reverse`.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authority única (`type`) ou página (`content`, `precedingElements`, `followingElements`, `reverse`)",
                    content = @Content(schema = @Schema(implementation = AuthorityPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos"),
            @ApiResponse(responseCode = "404", description = "Nenhum resultado para os critérios informados")
    })
    @GetMapping
    ResponseEntity<?> getAuthorities(
            @Parameter(description = "Identificador do perfil (obrigatório)")
            @RequestParam(required = false) UUID profileId,
            @Parameter(description = "Tipo da authority para busca de um único registro")
            @RequestParam(required = false) String type,
            @Parameter(description = "Cursor: último tipo visto na listagem")
            @RequestParam(required = false) String lastSeenType,
            @Parameter(description = "Tamanho máximo da página (1–100)", example = "100")
            @RequestParam(defaultValue = "100", required = false) int limit,
            @Parameter(description = "Ordenação descendente quando true", example = "false")
            @RequestParam(defaultValue = "false", required = false) boolean reverse);

}
