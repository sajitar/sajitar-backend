package com.sajitar.backend.adapter.in.web.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
                description = "Corpo inválido (validação de domínio)",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "E-mail já registrado para outro perfil",
                content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
})
public @interface ProfileWriteErrorResponses {

}
