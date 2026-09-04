package com.sajitar.backend.adapter.out.persistence.note;

import com.sajitar.backend.domain.model.note.Note;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class TypeConverter implements AttributeConverter<Note.Type, Short> {

    @Override
    public Short convertToDatabaseColumn(final Note.Type attribute) {
        return attribute == null ? null : (short) attribute.value();
    }

    @Override
    public Note.Type convertToEntityAttribute(final Short dbData) {
        return dbData == null ? null : Note.Type.valueOf(dbData.intValue());
    }

}
