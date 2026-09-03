package com.sajitar.backend.adapter.in.web.contract.profile;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Perfis", description = "Operações de criação, atualização, exclusão e consulta de perfis.")
@RequestMapping(value = Routes.PROFILE, produces = { APPLICATION_JSON_VALUE })
public interface ProfileApi {

    @Operation(
            summary = "Criar perfil",
            description = "Cria um novo perfil. O identificador é gerado pelo servidor e não deve ser enviado no corpo.")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil criado com sucesso",
            content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class)))
    @ProfileWriteErrorResponses
    @PostMapping
    ResponseEntity<ProfileSummaryResponse> postProfile(@Valid @RequestBody CreateProfileRequest request);

    @Operation(
            summary = "Atualizar perfil",
            description = """
                    Substitui um perfil existente. O identificador vem exclusivamente da URL e não pode ser alterado. \
                    Quando a senha é informada, ela é recodificada antes da persistência. Quando omitida, a senha atual é mantida.""")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    @ProfileWriteErrorResponses
    @PutMapping("/{id}")
    ResponseEntity<ProfileSummaryResponse> putProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request);

    @Operation(
            summary = "Atualizar perfil parcialmente",
            description = """
                    Atualiza apenas os campos enviados no corpo. Campos omitidos permanecem inalterados. \
                    O identificador vem exclusivamente da URL e não pode ser alterado. \
                    Descrição nula remove o valor atual. Senha omitida, nula ou em branco mantém o hash atual.""")
    @ApiResponse(
            responseCode = "200",
            description = "Perfil atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class)))
    @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    @ProfileWriteErrorResponses
    @PatchMapping("/{id}")
    ResponseEntity<ProfileSummaryResponse> patchProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody PatchProfileRequest request);

    @Operation(
            summary = "Excluir perfil",
            description = "Remove o perfil identificado pela URL. O identificador não pode ser alterado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id);

    @Operation(summary = "Obter perfil por id", description = "Retorna a visão resumida (id, nome e descrição) de um perfil.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil encontrado",
                    content = @Content(schema = @Schema(implementation = ProfileSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @GetMapping("/{id}")
    ResponseEntity<ProfileSummaryResponse> getProfile(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id);

    @Operation(
            summary = "Obter detalhes do perfil",
            description = "Retorna os detalhes completos de um perfil, incluindo e-mail e data de nascimento.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil encontrado",
                    content = @Content(schema = @Schema(implementation = ProfileDetailsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id na URL não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @GetMapping("/{id}/details")
    ResponseEntity<ProfileDetailsResponse> getProfileDetails(
            @Parameter(description = "Identificador do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id);

    @Operation(
            summary = "Listar perfis",
            description = """
                    Lista perfis com paginação por cursor. Sem parâmetro `name`, lista todos os perfis; \
                    com `name`, filtra por substring no nome (case-insensitive). \
                    Cursor completo (`lastSeenName` + `lastSeenId`) avança a página.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Página retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = ProfilePageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos"),
            @ApiResponse(responseCode = "404", description = "Nenhum resultado para os critérios informados")
    })
    @GetMapping
    ResponseEntity<ProfilePageResponse> getProfiles(
            @Parameter(description = "Substring para busca no nome (opcional)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Nome do último item visto (cursor)")
            @RequestParam(required = false) String lastSeenName,
            @Parameter(description = "Id do último item visto (cursor)")
            @RequestParam(required = false) UUID lastSeenId,
            @Parameter(description = "Tamanho máximo da página (1–100)", example = "100")
            @RequestParam(defaultValue = "100", required = false) int limit,
            @Parameter(description = "Ordenação descendente quando true", example = "false")
            @RequestParam(defaultValue = "false", required = false) boolean reverse);

}
