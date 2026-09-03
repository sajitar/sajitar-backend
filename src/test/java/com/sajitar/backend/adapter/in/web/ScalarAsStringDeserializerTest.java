package com.sajitar.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.adapter.in.web.contract.checker.CreateCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.checker.PatchCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.checker.UpdateCheckerRequest;
import com.sajitar.backend.domain.model.checker.Checker;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScalarAsStringDeserializer")
class ScalarAsStringDeserializerTest {

    @Mock
    private JsonParser parser;

    private static JsonMapper mapper() {
        return JsonMapper.builder().build();
    }

    @Test
    @DisplayName("String JSON vira o tipo enumerado")
    void stringTypeIsParsed() {
        final var request = mapper().readValue("{\"type\":\"CHANGE_EMAIL\"}", CreateCheckerRequest.class);
        assertThat(request.type()).isEqualTo("CHANGE_EMAIL");
        assertThat(request.toCommand(CheckerUseCaseProfileId.ID).type()).isEqualTo(Checker.Type.CHANGE_EMAIL);
    }

    @Test
    @DisplayName("Número JSON vira texto")
    void numberTypeIsReadAsString() {
        final var request = mapper().readValue("{\"type\":4}", CreateCheckerRequest.class);
        assertThat(request.type()).isEqualTo("4");
        assertThat(request.toCommand(CheckerUseCaseProfileId.ID).type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
    }

    @Test
    @DisplayName("Booleano JSON usa getValueAsString")
    void booleanUsesValueAsString() {
        final var request = mapper().readValue("{\"type\":true}", CreateCheckerRequest.class);
        assertThat(request.type()).isEqualTo("true");
    }

    @Test
    @DisplayName("type nulo no JSON vira null")
    void nullType() {
        final var request = mapper().readValue("{\"type\":null}", CreateCheckerRequest.class);
        assertThat(request.type()).isNull();
    }

    @Test
    @DisplayName("deserialize com token null devolve null")
    void deserializeNullToken() {
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);
        assertThat(new ScalarAsStringDeserializer().deserialize(parser, null)).isNull();
    }

    @Test
    @DisplayName("deserialize com número inteiro usa getIntValue")
    void deserializeIntToken() {
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
        when(parser.getIntValue()).thenReturn(2);
        assertThat(new ScalarAsStringDeserializer().deserialize(parser, null)).isEqualTo("2");
    }

    @Test
    @DisplayName("deserialize com string usa getString")
    void deserializeStringToken() {
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
        when(parser.getString()).thenReturn("CHANGE_EMAIL");
        assertThat(new ScalarAsStringDeserializer().deserialize(parser, null)).isEqualTo("CHANGE_EMAIL");
    }

    @Test
    @DisplayName("PUT ignora id no JSON e omite campos")
    void updateIgnoresUnknownAndOmits() {
        final var request = mapper().readValue(
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"code\":\"123456\"}",
                UpdateCheckerRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.id()).isEqualTo(CheckerUseCaseProfileId.ID);
        assertThat(command.code()).isEqualTo("123456");
        assertThat(command.payload()).isNull();
        assertThat(command.attempts()).isNull();
        assertThat(command.replaces()).isNull();
    }

    @Test
    @DisplayName("PATCH vazio deixa todos nulos")
    void emptyPatchIsAllNull() {
        final var request = mapper().readValue("{}", PatchCheckerRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isFalse();
    }

    private static final class CheckerUseCaseProfileId {
        static final java.util.UUID ID = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    }

}
