package com.sajitar.backend.adapter.in.web.contract.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.PatchValueDeserializer;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.profile.PatchProfileCommand;
import com.sajitar.backend.domain.model.profile.Profile;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchProfileRequest",
        description = "Corpo da atualização parcial. Campos omitidos permanecem inalterados. O identificador não é aceito no corpo. E-mail extra é ignorado. type omitido ou null mantém o vigente. Só um caller MASTER substitui o vigente por um valor diferente. Quando o type muda e o vigente ou o pedido é MASTER, password do caller é obrigatória. Fora dessa fronteira a senha extra é ignorada. twoFactor omitido mantém; null responde 400. twoFactor o próprio ou um caller MASTER em alvo WRITER/READER; twoFactor de outro MASTER responde 403 {id}. MASTER com twoFactor efetivo false responde 400.")
public record PatchProfileRequest(
        @Schema(description = "Tipo do perfil. Omitir ou null mantém o atual.", example = "WRITER")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        String type,
        @Schema(description = "Nome do perfil. Omitir para manter o atual.", example = "Maria Silva")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> name,
        @Schema(description = "Descrição do perfil. Omitir para manter; null remove a descrição.", example = "Uma pessoa criativa e dedicada.")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> description,
        @Schema(description = "Data de nascimento. Omitir para manter a atual.", example = "1988-01-10")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<LocalDate> birthday,
        @Schema(description = "Autenticação de dois fatores. Omitir para manter; null responde 400.", example = "true")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<Boolean> twoFactor,
        @Schema(
                description = "Senha do caller em texto plano, exigida quando o type cruza MASTER. Não altera o hash persistido.",
                example = "senhaSegura1")
        String password) {

    public PatchProfileCommand toCommand(final UUID id, final String address) {
        return new PatchProfileCommand(
                id,
                type == null ? null : Profile.Type.parse(type),
                name,
                description,
                birthday,
                twoFactor,
                password,
                address);
    }

}
