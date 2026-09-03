package com.sajitar.backend.adapter.in.web.contract.checker;

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

@Tag(name = "Checkers", description = "Operações de criação, atualização, exclusão e consulta de desafios de verificação.")
@RequestMapping(value = Routes.CHECKER, produces = { APPLICATION_JSON_VALUE })
public interface CheckerApi {

    @Operation(
            summary = "Criar checker",
            description = "Cria um checker para o perfil informado na query. VERIFY_EMAIL não pode ser criado pela API pública.")
    @ApiResponse(
            responseCode = "200",
            description = "Checker criado com sucesso",
            content = @Content(schema = @Schema(implementation = CheckerResponse.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Tipo não pode ser criado pela API pública (VERIFY_EMAIL)",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Perfil informado na query não existe",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @CheckerWriteErrorResponses
    @PostMapping
    ResponseEntity<CheckerResponse> postChecker(
            @Parameter(description = "Identificador do perfil dono do checker")
            @RequestParam(required = false) UUID profileId,
            @Valid @RequestBody CreateCheckerRequest request);

    @Operation(
            summary = "Atualizar checker",
            description = """
                    Substitui os campos mutáveis. Campos omitidos ou nulos voltam aos defaults de criação \
                    (código gerado, payload nulo, attempts 10, replaces 3). O identificador vem exclusivamente da URL.""")
    @ApiResponse(
            responseCode = "200",
            description = "Checker atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = CheckerResponse.class)))
    @ApiResponse(responseCode = "404", description = "Checker não encontrado")
    @CheckerWriteErrorResponses
    @PutMapping("/{id}")
    ResponseEntity<CheckerResponse> putChecker(
            @Parameter(description = "Identificador do checker", example = "019c1000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCheckerRequest request);

    @Operation(
            summary = "Atualizar checker parcialmente",
            description = """
                    Atualiza apenas os campos não nulos enviados no corpo. Campos omitidos ou nulos permanecem inalterados. \
                    O identificador vem exclusivamente da URL.""")
    @ApiResponse(
            responseCode = "200",
            description = "Checker atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = CheckerResponse.class)))
    @ApiResponse(responseCode = "404", description = "Checker não encontrado")
    @CheckerWriteErrorResponses
    @PatchMapping("/{id}")
    ResponseEntity<CheckerResponse> patchChecker(
            @Parameter(description = "Identificador do checker", example = "019c1000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Valid @RequestBody PatchCheckerRequest request);

    @Operation(
            summary = "Excluir checker",
            description = "Remove o checker identificado pela URL. O identificador não pode ser alterado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Checker excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Checker não encontrado")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteChecker(
            @Parameter(description = "Identificador do checker", example = "019c1000-a111-7000-8000-111111111111")
            @PathVariable UUID id);

    @Operation(summary = "Obter checker por id", description = "Retorna o checker completo, incluindo código e payload.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Checker encontrado",
                    content = @Content(schema = @Schema(implementation = CheckerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Checker não encontrado")
    })
    @GetMapping("/{id}")
    ResponseEntity<CheckerResponse> getChecker(
            @Parameter(description = "Identificador do checker", example = "019c1000-a111-7000-8000-111111111111")
            @PathVariable UUID id);

    @Operation(
            summary = "Consultar checkers do perfil",
            description = """
                    Com `type`, devolve um único checker do par (profileId, type) e ignora \
                    `lastSeenType`, `limit` e `reverse`. Sem `type`, lista os checkers do perfil com paginação \
                    por cursor (`lastSeenType`, `limit`, `reverse`). \
                    A página JSON contém só `content`, `precedingElements`, `followingElements` e `reverse`.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Checker único (`type`) ou página (`content`, `precedingElements`, `followingElements`, `reverse`)",
                    content = @Content(schema = @Schema(implementation = CheckerPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos"),
            @ApiResponse(responseCode = "404", description = "Nenhum resultado para os critérios informados")
    })
    @GetMapping
    ResponseEntity<?> getCheckers(
            @Parameter(description = "Identificador do perfil (obrigatório)")
            @RequestParam(required = false) UUID profileId,
            @Parameter(description = "Tipo do checker para busca de um único registro")
            @RequestParam(required = false) String type,
            @Parameter(description = "Cursor: último tipo visto na listagem")
            @RequestParam(required = false) String lastSeenType,
            @Parameter(description = "Tamanho máximo da página (1–100)", example = "100")
            @RequestParam(defaultValue = "100", required = false) int limit,
            @Parameter(description = "Ordenação descendente quando true", example = "false")
            @RequestParam(defaultValue = "false", required = false) boolean reverse);

}
