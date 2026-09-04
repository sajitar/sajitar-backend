package com.sajitar.backend.adapter.in.web.contract.note;

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

@Tag(name = "Notes", description = "Operações de criação, atualização, exclusão e consulta de notas de um perfil.")
@RequestMapping(value = Routes.NOTE, produces = { APPLICATION_JSON_VALUE })
public interface NoteApi {

    @Operation(
            summary = "Criar nota",
            description = "Cria uma nota para o perfil informado na query. Corpo: type e content. Tipos: PUBLIC, PROTECTED e PRIVATE. Um perfil pode ter várias notas do mesmo tipo.")
    @ApiResponse(
            responseCode = "200",
            description = "Nota criada com sucesso",
            content = @Content(schema = @Schema(implementation = NoteResponse.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Perfil informado na query não existe",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @NoteWriteErrorResponses
    @PostMapping
    ResponseEntity<NoteResponse> postNote(
            @Parameter(description = "Identificador do perfil dono da nota")
            @RequestParam(required = false) UUID profileId,
            @Valid @RequestBody CreateNoteRequest request);

    @Operation(
            summary = "Atualizar nota",
            description = """
                    Substitui type e content. Sem mudança real o servidor não grava. \
                    O identificador vem exclusivamente da URL.""")
    @ApiResponse(
            responseCode = "200",
            description = "Nota atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = NoteResponse.class)))
    @ApiResponse(responseCode = "404", description = "Nota não encontrada")
    @NoteWriteErrorResponses
    @PutMapping("/{id}")
    ResponseEntity<NoteResponse> putNote(
            @Parameter(description = "Identificador da nota", example = "019c3000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteRequest request);

    @Operation(
            summary = "Atualizar nota parcialmente",
            description = """
                    Atualiza type e/ou content. Campo omitido permanece inalterado; content nulo ou em branco retorna 400. \
                    Sem mudança real o servidor não grava. \
                    O identificador vem exclusivamente da URL.""")
    @ApiResponse(
            responseCode = "200",
            description = "Nota atualizada com sucesso",
            content = @Content(schema = @Schema(implementation = NoteResponse.class)))
    @ApiResponse(responseCode = "404", description = "Nota não encontrada")
    @NoteWriteErrorResponses
    @PatchMapping("/{id}")
    ResponseEntity<NoteResponse> patchNote(
            @Parameter(description = "Identificador da nota", example = "019c3000-a111-7000-8000-111111111111")
            @PathVariable UUID id,
            @Valid @RequestBody PatchNoteRequest request);

    @Operation(
            summary = "Excluir nota",
            description = "Remove a nota identificada pela URL. O identificador não pode ser alterado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Nota excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Nota não encontrada")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteNote(
            @Parameter(description = "Identificador da nota", example = "019c3000-a111-7000-8000-111111111111")
            @PathVariable UUID id);

    @Operation(summary = "Obter nota por id", description = "Retorna a nota completa.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nota encontrada",
                    content = @Content(schema = @Schema(implementation = NoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Nota não encontrada")
    })
    @GetMapping("/{id}")
    ResponseEntity<NoteResponse> getNote(
            @Parameter(description = "Identificador da nota", example = "019c3000-a111-7000-8000-111111111111")
            @PathVariable UUID id);

    @Operation(
            summary = "Listar notas do perfil",
            description = """
                    Lista as notas do perfil com paginação por cursor (`lastSeenId`, `limit`, `reverse`). \
                    `type` é filtro opcional; a resposta continua sendo página. \
                    A página JSON contém só `content`, `precedingElements`, `followingElements` e `reverse`.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página (`content`, `precedingElements`, `followingElements`, `reverse`)",
                    content = @Content(schema = @Schema(implementation = NotePageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos"),
            @ApiResponse(responseCode = "404", description = "Nenhum resultado para os critérios informados")
    })
    @GetMapping
    ResponseEntity<NotePageResponse> getNotes(
            @Parameter(description = "Identificador do perfil (obrigatório)")
            @RequestParam(required = false) UUID profileId,
            @Parameter(description = "Filtro opcional de tipo; a resposta continua sendo página")
            @RequestParam(required = false) String type,
            @Parameter(description = "Cursor: último id visto na listagem")
            @RequestParam(required = false) UUID lastSeenId,
            @Parameter(description = "Tamanho máximo da página (1–100)", example = "100")
            @RequestParam(defaultValue = "100", required = false) int limit,
            @Parameter(description = "Ordenação descendente quando true", example = "false")
            @RequestParam(defaultValue = "false", required = false) boolean reverse);

}
