package com.sajitar.backend.adapter.in.web;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public final class ScalarAsStringDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(final JsonParser parser, final DeserializationContext context) {
        final var token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return Integer.toString(parser.getIntValue());
        }
        if (token == JsonToken.VALUE_STRING) {
            return parser.getString();
        }
        return parser.getValueAsString();
    }

}
