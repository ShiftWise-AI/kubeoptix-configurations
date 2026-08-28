package com.shiftwise.ai.kubeoptix.documents;

import java.util.UUID;

public record DocumentRequest(
        String documentName,
        String title,
        String projectManager,
        String costumer,
        UUID authorId,
        UUID costumersListId) {
}