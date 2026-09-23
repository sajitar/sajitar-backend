package com.sajitar.backend.adapter.in.web.contract.profile;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sajitar.backend.adapter.in.web.contract.ValidationErrorResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Senha atual ou nova mal formada, ou iguais",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Bearer ausente ou inválido, ou senha atual que não confere",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Perfil da sessão não encontrado"),
        @ApiResponse(
                responseCode = "429",
                description = "Limite de tentativas; Retry-After indica a espera",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Store de sessões indisponível")
})
public @interface ChangeOwnPasswordErrorResponses {

}
