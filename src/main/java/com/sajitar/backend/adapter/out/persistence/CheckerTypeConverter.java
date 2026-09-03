package com.sajitar.backend.adapter.out.persistence;

import com.sajitar.backend.domain.model.Checker;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class CheckerTypeConverter implements AttributeConverter<Checker.Type, Short> {

    @Override
    public Short convertToDatabaseColumn(final Checker.Type attribute) {
        return attribute == null ? null : (short) attribute.value();
    }

    @Override
    public Checker.Type convertToEntityAttribute(final Short dbData) {
        return dbData == null ? null : Checker.Type.valueOf(dbData.intValue());
    }

}
