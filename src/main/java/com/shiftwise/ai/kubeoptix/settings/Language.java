package com.shiftwise.ai.kubeoptix.settings;

import java.util.Locale;
import java.util.Locale.IllformedLocaleException;

public final class Language {

    public static final String DEFAULT = "en";

    private Language() {
    }

    public static String validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Language must be a valid BCP 47 language tag");
        }

        try {
            Locale locale = new Locale.Builder().setLanguageTag(value).build();
            if (locale.toLanguageTag().equals("und")) {
                throw new IllegalArgumentException("Language must contain a language subtag");
            }
            return locale.toLanguageTag();
        } catch (IllformedLocaleException exception) {
            throw new IllegalArgumentException("Invalid BCP 47 language tag: " + value, exception);
        }
    }
}