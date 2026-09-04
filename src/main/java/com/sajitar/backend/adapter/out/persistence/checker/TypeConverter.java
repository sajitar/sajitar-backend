package com.sajitar.backend.adapter.out.persistence.checker;

import com.sajitar.backend.domain.model.checker.Checker;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class TypeConverter implements AttributeConverter<Checker.Type, Short> {

    @Override
    public Short convertToDatabaseColumn(final Checker.Type attribute) {
        return attribute == null ? null : (short) attribute.value();
    }

    @Override
    public Checker.Type convertToEntityAttribute(final Short dbData) {
        return dbData == null ? null : Checker.Type.valueOf(dbData.intValue());
    }

}
