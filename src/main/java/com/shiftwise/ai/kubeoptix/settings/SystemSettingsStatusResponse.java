package com.shiftwise.ai.kubeoptix.settings;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SystemSettingsStatusResponse(
	@Schema(description = "Settings activation status.", example = "active", enumeration = { "active", "inactive" })
	SettingsStatus status) {
}