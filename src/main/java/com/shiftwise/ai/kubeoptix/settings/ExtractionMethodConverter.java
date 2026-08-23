package com.shiftwise.ai.kubeoptix.settings;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ExtractionMethodConverter implements AttributeConverter<ExtractionMethod, String> {

    @Override
    public String convertToDatabaseColumn(ExtractionMethod attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ExtractionMethod convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ExtractionMethod.fromValue(dbData);
    }
}