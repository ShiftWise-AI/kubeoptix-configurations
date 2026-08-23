package com.shiftwise.ai.kubeoptix.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExtractionMethod {
    ML("ml"),
    LLM("llm");

    private final String value;

    ExtractionMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ExtractionMethod fromValue(String value) {
        for (ExtractionMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown extraction method: " + value);
    }
}