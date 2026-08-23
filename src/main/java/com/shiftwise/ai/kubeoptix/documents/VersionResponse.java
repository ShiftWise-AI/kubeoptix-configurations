package com.shiftwise.ai.kubeoptix.documents;

import java.time.LocalDateTime;
import java.util.UUID;

public record VersionResponse(UUID id, String versionNumber, String markdownContent, LocalDateTime createdAt) {

    static VersionResponse from(Version version) {
        return new VersionResponse(version.id, version.versionNumber, version.markdownContent, version.createdAt);
    }
}