package com.sajitar.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.adapter.in.web.contract.PatchProfileRequest;
import com.sajitar.backend.application.command.PatchValue;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchValueDeserializer")
class PatchValueDeserializerTest {

    private static final UUID PATH_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private JsonParser parser;

    @Mock
    private DeserializationContext context;

    private static JsonMapper mapper() {
        return JsonMapper.builder().build();
    }

    @Test
    @DisplayName("JSON vazio deixa todos os campos ausentes")
    void emptyObjectLeavesAllFieldsAbsent() {
        final var request = mapper().readValue("{}", PatchProfileRequest.class);

        assertThat(request.name()).isEqualTo(PatchValue.absent());
        assertThat(request.description()).isEqualTo(PatchValue.absent());
        assertThat(request.birthday()).isEqualTo(PatchValue.absent());
        assertThat(request.email()).isEqualTo(PatchValue.absent());
        assertThat(request.password()).isEqualTo(PatchValue.absent());
        final var command = request.toCommand(PATH_ID);
        assertThat(command.id()).isEqualTo(PATH_ID);
        assertThat(command.name().isPresent()).isFalse();
    }

    @Test
    @DisplayName("Nome presente substitui apenas esse campo")
    void presentNameIsOfValue() {
        final var request = mapper().readValue("{\"name\":\"Maria Silva\"}", PatchProfileRequest.class);

        assertThat(request.name()).isEqualTo(PatchValue.of("Maria Silva"));
        assertThat(request.description().isPresent()).isFalse();
    }

    @Test
    @DisplayName("description nula é presença com null, não ausência")
    void explicitNullDescriptionIsPresentNull() {
        final var request = mapper().readValue("{\"description\":null}", PatchProfileRequest.class);

        assertThat(request.description()).isEqualTo(PatchValue.of(null));
        assertThat(request.description().isPresent()).isTrue();
    }

    @Test
    @DisplayName("birthday ISO vira LocalDate presente")
    void isoBirthdayIsPresentLocalDate() {
        final var request = mapper().readValue("{\"birthday\":\"1988-01-10\"}", PatchProfileRequest.class);

        assertThat(request.birthday()).isEqualTo(PatchValue.of(LocalDate.parse("1988-01-10")));
    }

    @Test
    @DisplayName("id no JSON é ignorado")
    void unknownIdPropertyIsIgnored() {
        final var request = mapper().readValue(
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"name\":\"Maria Silva\"}",
                PatchProfileRequest.class);

        assertThat(request.toCommand(PATH_ID).id()).isEqualTo(PATH_ID);
        assertThat(request.name()).isEqualTo(PatchValue.of("Maria Silva"));
    }

    @Test
    @DisplayName("createContextual sem propriedade devolve o mesmo deserializer")
    void createContextualWithoutPropertyReturnsThis() {
        final var deserializer = new PatchValueDeserializer();

        assertThat(deserializer.createContextual(context, null)).isSameAs(deserializer);
    }

    @Test
    @DisplayName("getNullValue e getAbsentValue distinguem null explícito de campo omitido")
    void nullAndAbsentValues() {
        final var deserializer = new PatchValueDeserializer();

        assertThat(deserializer.getNullValue(context)).isEqualTo(PatchValue.of(null));
        assertThat(deserializer.getAbsentValue(context)).isEqualTo(PatchValue.absent());
    }

    @Test
    @DisplayName("deserialize com token null devolve PatchValue.of(null)")
    void deserializeNullToken() {
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);

        assertThat(new PatchValueDeserializer().deserialize(parser, context)).isEqualTo(PatchValue.of(null));
    }

}
