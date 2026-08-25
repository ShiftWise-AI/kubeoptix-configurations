package com.shiftwise.ai.kubeoptix.documents;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id,
        String title,
        String projectManager,
        String costumer,
        UUID authorId,
        UUID costumersListId,
        UUID versionId,
        LocalDateTime createdAt) {

    static DocumentVersionResponse from(DocumentVersion documentVersion) {
        return new DocumentVersionResponse(
                documentVersion.id,
                documentVersion.title,
                documentVersion.projectManager,
                documentVersion.costumer,
                documentVersion.author.id,
                documentVersion.customer.id,
                documentVersion.version.id,
                documentVersion.createdAt);
    }
}