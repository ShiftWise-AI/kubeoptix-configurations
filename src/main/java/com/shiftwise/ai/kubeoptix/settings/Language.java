package com.shiftwise.ai.kubeoptix.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Language {
    EN("en"),
    PT("pt"),
    ES("es"),
    IT("it");

    private final String value;

    Language(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Language fromValue(String value) {
        for (Language language : values()) {
            if (language.value.equals(value)) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unknown language: " + value);
    }
}