package com.shiftwise.ai.kubeoptix.settings;

import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SystemSettingsResponse(
    @Schema(description = "System settings unique identifier.", example = "7a0c6a7a-01b7-4da8-8c8a-d4389f693d78")
        UUID id,
    @Schema(description = "Default language used by the application.", example = "pt")
        Language language,
    @Schema(description = "Cursor API key used by integrations.", example = "cursor-api-key")
        String cursorApiKey,
    @Schema(description = "Cursor model name.", example = "gpt-4.1")
        String cursorModel,
    @Schema(description = "External LLM provider API key.", example = "llm-api-key")
        String llmApiKey,
    @Schema(description = "External LLM provider model name.", example = "llama-3.3-70b")
        String llmModel,
    @Schema(description = "Settings activation status.", example = "active")
        SettingsStatus status,
    @Schema(description = "Default extraction method.", example = "llm")
        ExtractionMethod defaultExtractionMethod,
    @Schema(description = "Record creation timestamp.", example = "2026-08-22T21:30:00")
        LocalDateTime createdAt) {

    public static SystemSettingsResponse from(SystemSettings settings) {
        return new SystemSettingsResponse(
                settings.id,
                settings.language,
                settings.cursorApiKey,
                settings.cursorModel,
                settings.llmApiKey,
                settings.llmModel,
                settings.status,
                settings.defaultExtractionMethod,
                settings.createdAt);
    }
}