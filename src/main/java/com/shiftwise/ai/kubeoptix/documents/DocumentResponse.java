package com.shiftwise.ai.kubeoptix.documents;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        String documentName,
        String title,
        String projectManager,
        String costumer,
        UUID authorId,
        UUID costumersListId,
        String markdownContent,
        LocalDateTime createdAt) {

    static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.documentName,
                document.title,
                document.projectManager,
                document.costumer,
                document.author.id,
                document.customer.id,
                document.markdownContent,
                document.createdAt);
    }
}