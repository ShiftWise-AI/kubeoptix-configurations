package com.shiftwise.ai.kubeoptix.settings;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/system-settings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "System Settings", description = "Manage the single system settings record.")
public class SystemSettingsResource {

    @PUT
    @Transactional
        @Operation(summary = "Create or update system settings", description = "Creates the only system settings record when it does not exist, or updates the existing record.")
        @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SystemSettingsRequest.class), examples = @ExampleObject(name = "Settings payload", value = """
                        {
                            "language": "pt",
                            "cursorApiKey": "cursor-api-key",
                            "cursorModel": "gpt-4.1",
                            "llmApiKey": "llm-api-key",
                            "llmModel": "llama-3.3-70b",
                            "status": "active",
                            "defaultExtractionMethod": "llm"
                        }
                        """)))
        @APIResponse(responseCode = "200", description = "System settings saved or updated.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SystemSettingsResponse.class)))
    public SystemSettingsResponse saveOrUpdate(SystemSettingsRequest request) {
        SystemSettings settings = findSettings();
        if (settings == null) {
            settings = new SystemSettings();
            apply(request, settings);
            settings.persist();
            return SystemSettingsResponse.from(settings);
        }

        apply(request, settings);
        return SystemSettingsResponse.from(settings);
    }

    @GET
    @Path("/status")
    @Operation(summary = "Get system settings status", description = "Returns the status field from the single system settings record.")
    @APIResponse(responseCode = "200", description = "Current settings status.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SystemSettingsStatusResponse.class)))
    @APIResponse(responseCode = "404", description = "System settings were not configured yet.")
    public SystemSettingsStatusResponse status() {
        return new SystemSettingsStatusResponse(requiredSettings().status);
    }

    @GET
    @Operation(summary = "Get system settings", description = "Returns all fields from the single system settings record.")
    @APIResponse(responseCode = "200", description = "Current system settings.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SystemSettingsResponse.class)))
    @APIResponse(responseCode = "404", description = "System settings were not configured yet.")
    public SystemSettingsResponse findAllFields() {
        return SystemSettingsResponse.from(requiredSettings());
    }

    private static SystemSettings findSettings() {
        return SystemSettings.findAll().firstResult();
    }

    private static SystemSettings requiredSettings() {
        SystemSettings settings = findSettings();
        if (settings == null) {
            throw new NotFoundException("System settings were not configured yet");
        }
        return settings;
    }

    private static void apply(SystemSettingsRequest request, SystemSettings settings) {
        settings.language = request.language() == null ? Language.EN : request.language();
        settings.cursorApiKey = request.cursorApiKey();
        settings.cursorModel = request.cursorModel();
        settings.llmApiKey = request.llmApiKey();
        settings.llmModel = request.llmModel();
        settings.status = request.status() == null ? SettingsStatus.ACTIVE : request.status();
        settings.defaultExtractionMethod = request.defaultExtractionMethod() == null ? ExtractionMethod.ML
                : request.defaultExtractionMethod();
    }
}