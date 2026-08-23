package com.shiftwise.ai.kubeoptix.settings;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SystemSettingsRequest(
        @Schema(description = "Default language used by the application.", example = "pt", enumeration = { "en", "pt",
                "es", "it" })
        Language language,
        @Schema(description = "Cursor API key used by integrations.", example = "cursor-api-key")
        String cursorApiKey,
        @Schema(description = "Cursor model name.", example = "gpt-4.1")
        String cursorModel,
        @Schema(description = "External LLM provider API key.", example = "llm-api-key")
        String llmApiKey,
        @Schema(description = "External LLM provider model name.", example = "llama-3.3-70b")
        String llmModel,
        @Schema(description = "Settings activation status.", example = "active", enumeration = { "active", "inactive" })
        SettingsStatus status,
        @Schema(description = "Default extraction method.", example = "llm", enumeration = { "ml", "llm" })
        ExtractionMethod defaultExtractionMethod) {
}