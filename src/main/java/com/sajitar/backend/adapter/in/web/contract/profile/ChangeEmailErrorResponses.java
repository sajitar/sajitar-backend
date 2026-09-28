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
                description = "Código mal formado",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Bearer ausente ou inválido, checker ausente, vencido, payload nulo ou código que não confere",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "E-mail ainda não verificado (checker VERIFY_EMAIL)",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Perfil da sessão não encontrado"),
        @ApiResponse(
                responseCode = "409",
                description = "E-mail novo já registrado para outro perfil",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "429",
                description = "Limite de tentativas; Retry-After indica a espera",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Store de sessões indisponível")
})
public @interface ChangeEmailErrorResponses {

}
