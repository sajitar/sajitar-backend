package com.sajitar.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.adapter.in.web.contract.authority.CreateAuthorityRequest;
import com.sajitar.backend.adapter.in.web.contract.authority.PatchAuthorityRequest;
import com.sajitar.backend.adapter.in.web.contract.authority.UpdateAuthorityRequest;
import com.sajitar.backend.adapter.in.web.contract.checker.CreateCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.checker.PatchCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.checker.UpdateCheckerRequest;
import com.sajitar.backend.adapter.in.web.contract.note.CreateNoteRequest;
import com.sajitar.backend.adapter.in.web.contract.note.PatchNoteRequest;
import com.sajitar.backend.adapter.in.web.contract.note.UpdateNoteRequest;
import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.note.Note;

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
        final var request = mapper().readValue("{\"type\":2}", CreateCheckerRequest.class);
        assertThat(request.type()).isEqualTo("2");
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
    @DisplayName("PUT aceita type e payload e ignora id no JSON")
    void updateIgnoresUnknownAndKeepsTypePayload() {
        final var request = mapper().readValue(
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"type\":\"CHANGE_EMAIL\",\"payload\":\"x\",\"code\":\"123456\"}",
                UpdateCheckerRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.id()).isEqualTo(CheckerUseCaseProfileId.ID);
        assertThat(command.type()).isEqualTo(Checker.Type.CHANGE_EMAIL);
        assertThat(command.payload()).isEqualTo("x");
    }

    @Test
    @DisplayName("PATCH vazio deixa type nulo e payload ausente")
    void emptyPatchIsAllNull() {
        final var request = mapper().readValue("{}", PatchCheckerRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isFalse();
        assertThat(command.type()).isNull();
        assertThat(command.payload()).isEqualTo(PatchValue.absent());
    }

    @Test
    @DisplayName("PATCH com type parseia o enum")
    void patchParsesType() {
        final var request = mapper().readValue("{\"type\":\"CHANGE_PASSWORD\",\"payload\":\"p\"}", PatchCheckerRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isTrue();
        assertThat(command.type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(command.payload()).isEqualTo(PatchValue.of("p"));
    }

    @Test
    @DisplayName("PATCH com payload nulo é presença com null")
    void patchNullPayloadIsPresentNull() {
        final var request = mapper().readValue("{\"payload\":null}", PatchCheckerRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isTrue();
        assertThat(command.payload()).isEqualTo(PatchValue.of(null));
    }

    @Test
    @DisplayName("Authority: string JSON vira o tipo enumerado")
    void authorityStringTypeIsParsed() {
        final var request = mapper().readValue("{\"type\":\"MASTER\"}", CreateAuthorityRequest.class);
        assertThat(request.type()).isEqualTo("MASTER");
        assertThat(request.toCommand(CheckerUseCaseProfileId.ID).type()).isEqualTo(Authority.Type.MASTER);
    }

    @Test
    @DisplayName("Authority: número JSON vira texto e parseia o enum")
    void authorityNumberTypeIsReadAsString() {
        final var request = mapper().readValue("{\"type\":2}", CreateAuthorityRequest.class);
        assertThat(request.type()).isEqualTo("2");
        assertThat(request.toCommand(CheckerUseCaseProfileId.ID).type()).isEqualTo(Authority.Type.READER);
    }

    @Test
    @DisplayName("Authority PUT aceita type e ignora id no JSON")
    void authorityUpdateIgnoresUnknownAndKeepsType() {
        final var request = mapper().readValue(
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"type\":\"MEMBER\"}",
                UpdateAuthorityRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.id()).isEqualTo(CheckerUseCaseProfileId.ID);
        assertThat(command.type()).isEqualTo(Authority.Type.MEMBER);
    }

    @Test
    @DisplayName("Authority PATCH vazio deixa type nulo")
    void authorityEmptyPatchIsAllNull() {
        final var request = mapper().readValue("{}", PatchAuthorityRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isFalse();
        assertThat(command.type()).isNull();
    }

    @Test
    @DisplayName("Authority PATCH com type parseia o enum")
    void authorityPatchParsesType() {
        final var request = mapper().readValue("{\"type\":\"READER\"}", PatchAuthorityRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isTrue();
        assertThat(command.type()).isEqualTo(Authority.Type.READER);
    }

    @Test
    @DisplayName("Note: string JSON vira o tipo enumerado")
    void noteStringTypeIsParsed() {
        final var request = mapper().readValue("{\"type\":\"PUBLIC\",\"content\":\"Uma nota.\"}", CreateNoteRequest.class);
        assertThat(request.type()).isEqualTo("PUBLIC");
        assertThat(request.toCommand(CheckerUseCaseProfileId.ID).type()).isEqualTo(Note.Type.PUBLIC);
        assertThat(request.toCommand(CheckerUseCaseProfileId.ID).content()).isEqualTo("Uma nota.");
    }

    @Test
    @DisplayName("Note: número JSON vira texto e parseia o enum")
    void noteNumberTypeIsReadAsString() {
        final var request = mapper().readValue("{\"type\":2,\"content\":\"Privada.\"}", CreateNoteRequest.class);
        assertThat(request.type()).isEqualTo("2");
        assertThat(request.toCommand(CheckerUseCaseProfileId.ID).type()).isEqualTo(Note.Type.PRIVATE);
    }

    @Test
    @DisplayName("Note PUT aceita type e content e ignora id no JSON")
    void noteUpdateIgnoresUnknownAndKeepsFields() {
        final var request = mapper().readValue(
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"type\":\"PROTECTED\",\"content\":\"Atualizada.\"}",
                UpdateNoteRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.id()).isEqualTo(CheckerUseCaseProfileId.ID);
        assertThat(command.type()).isEqualTo(Note.Type.PROTECTED);
        assertThat(command.content()).isEqualTo("Atualizada.");
    }

    @Test
    @DisplayName("Note PATCH vazio deixa type nulo e content ausente")
    void noteEmptyPatchIsAllNull() {
        final var request = mapper().readValue("{}", PatchNoteRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isFalse();
        assertThat(command.type()).isNull();
        assertThat(command.content()).isEqualTo(PatchValue.absent());
    }

    @Test
    @DisplayName("Note PATCH com type e content parseia o enum")
    void notePatchParsesType() {
        final var request = mapper().readValue("{\"type\":\"PRIVATE\",\"content\":\"p\"}", PatchNoteRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isTrue();
        assertThat(command.type()).isEqualTo(Note.Type.PRIVATE);
        assertThat(command.content()).isEqualTo(PatchValue.of("p"));
    }

    @Test
    @DisplayName("Note PATCH com content nulo é presença com null")
    void notePatchNullContentIsPresentNull() {
        final var request = mapper().readValue("{\"content\":null}", PatchNoteRequest.class);
        final var command = request.toCommand(CheckerUseCaseProfileId.ID);
        assertThat(command.hasChanges()).isTrue();
        assertThat(command.content()).isEqualTo(PatchValue.of(null));
    }

    private static final class CheckerUseCaseProfileId {
        static final java.util.UUID ID = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    }

}
