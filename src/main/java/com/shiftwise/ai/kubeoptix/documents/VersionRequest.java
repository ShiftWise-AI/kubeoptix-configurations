package com.shiftwise.ai.kubeoptix.documents;

public record VersionRequest(String versionNumber, String description, String markdownContent, String documentName) {
}