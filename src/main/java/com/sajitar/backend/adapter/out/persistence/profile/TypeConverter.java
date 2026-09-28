package com.sajitar.backend.adapter.out.persistence.profile;

import com.sajitar.backend.domain.model.profile.Profile;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class TypeConverter implements AttributeConverter<Profile.Type, Short> {

    @Override
    public Short convertToDatabaseColumn(final Profile.Type attribute) {
        return attribute == null ? null : (short) attribute.value();
    }

    @Override
    public Profile.Type convertToEntityAttribute(final Short dbData) {
        return dbData == null ? null : Profile.Type.valueOf(dbData.intValue());
    }

}
