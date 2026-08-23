package com.shiftwise.ai.kubeoptix.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SettingsStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    SettingsStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SettingsStatus fromValue(String value) {
        for (SettingsStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown settings status: " + value);
    }
}