package com.sajitar.backend.adapter.out.persistence.authority;

import com.sajitar.backend.domain.model.authority.Authority;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class AuthorityTypeConverter implements AttributeConverter<Authority.Type, Short> {

    @Override
    public Short convertToDatabaseColumn(final Authority.Type attribute) {
        return attribute == null ? null : (short) attribute.value();
    }

    @Override
    public Authority.Type convertToEntityAttribute(final Short dbData) {
        return dbData == null ? null : Authority.Type.valueOf(dbData.intValue());
    }

}
