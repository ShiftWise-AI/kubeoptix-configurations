package com.shiftwise.ai.kubeoptix.documents;

import java.util.UUID;

public record DocumentVersionRequest(
	String title,
	String projectManager,
	String costumer,
	UUID authorId,
	UUID costumersListId,
	UUID versionId) {
}