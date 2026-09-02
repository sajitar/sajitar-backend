package com.sajitar.backend.adapter.in.web;

import com.sajitar.backend.application.command.PatchValue;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;

public final class PatchValueDeserializer extends ValueDeserializer<PatchValue<?>> {

    private final JavaType valueType;

    public PatchValueDeserializer() {
        this.valueType = null;
    }

    PatchValueDeserializer(final JavaType valueType) {
        this.valueType = valueType;
    }

    @Override
    public ValueDeserializer<?> createContextual(final DeserializationContext ctxt, final BeanProperty property) {
        if (property == null) {
            return this;
        }
        return new PatchValueDeserializer(property.getType().containedType(0));
    }

    @Override
    public PatchValue<?> deserialize(final JsonParser parser, final DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return PatchValue.of(null);
        }
        return PatchValue.of(context.readValue(parser, valueType));
    }

    @Override
    public PatchValue<?> getNullValue(final DeserializationContext context) {
        return PatchValue.of(null);
    }

    @Override
    public Object getAbsentValue(final DeserializationContext context) {
        return PatchValue.absent();
    }

}
