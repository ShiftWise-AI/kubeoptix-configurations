package com.shiftwise.ai.kubeoptix.documents;

import java.util.UUID;

public record DocumentVersionRequest(String title, String projectManager, UUID authorId, UUID customerId, UUID versionId) {
}