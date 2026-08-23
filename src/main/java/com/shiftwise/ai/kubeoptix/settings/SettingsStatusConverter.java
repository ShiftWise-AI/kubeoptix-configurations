package com.shiftwise.ai.kubeoptix.settings;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SettingsStatusConverter implements AttributeConverter<SettingsStatus, String> {

    @Override
    public String convertToDatabaseColumn(SettingsStatus attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public SettingsStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SettingsStatus.fromValue(dbData);
    }
}