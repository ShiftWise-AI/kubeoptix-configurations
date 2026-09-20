package com.shiftwise.ai.kubeoptix.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LanguageTest {

    @Test
    void acceptsSimpleAndRegionalBcp47Tags() {
        assertEquals("en", Language.validate("en"));
        assertEquals("pt-BR", Language.validate("pt-BR"));
        assertEquals("zh-Hans", Language.validate("zh-Hans"));
    }

    @Test
    void rejectsInvalidTags() {
        assertThrows(IllegalArgumentException.class, () -> Language.validate("pt_BR"));
        assertThrows(IllegalArgumentException.class, () -> Language.validate("a"));
        assertThrows(IllegalArgumentException.class, () -> Language.validate(""));
    }
}
