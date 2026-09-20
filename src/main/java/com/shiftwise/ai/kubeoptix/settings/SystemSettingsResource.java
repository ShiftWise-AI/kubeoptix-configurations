package com.shiftwise.ai.kubeoptix.settings;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    @PATCH
    @Transactional
    @Operation(summary = "Update system settings", description = "Updates the existing system settings record. Only the provided fields are changed.")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SystemSettingsRequest.class), examples = @ExampleObject(name = "Partial settings payload", value = """
                    {
                        "llmModel": "llama-3.3-70b",
                        "status": "inactive"
                    }
                    """)))
    @APIResponse(responseCode = "200", description = "System settings updated.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SystemSettingsResponse.class)))
    @APIResponse(responseCode = "404", description = "System settings were not configured yet.")
    public SystemSettingsResponse update(SystemSettingsRequest request) {
        SystemSettings settings = requiredSettings();
        merge(request, settings);
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

    @PUT
    @Path("/logo")
    @Transactional
    @Consumes({ "image/png", "image/jpeg", "image/gif", "image/svg+xml", MediaType.APPLICATION_OCTET_STREAM })
    @Operation(summary = "Upload the system logo", description = "Stores the raw image bytes sent in the request body as the system logo.")
    @RequestBody(required = true, description = "Raw image bytes.", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "200", description = "Logo stored.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SystemSettingsResponse.class)))
    @APIResponse(responseCode = "400", description = "Empty request body.")
    @APIResponse(responseCode = "404", description = "System settings were not configured yet.")
    public SystemSettingsResponse uploadLogo(byte[] logo) {
        if (logo == null || logo.length == 0) {
            throw new BadRequestException("Logo content must not be empty");
        }
        SystemSettings settings = requiredSettings();
        settings.logo = logo;
        return SystemSettingsResponse.from(settings);
    }

    @GET
    @Path("/logo")
    @Produces({ "image/png", "image/jpeg", "image/gif", "image/svg+xml", MediaType.APPLICATION_OCTET_STREAM })
    @Operation(summary = "Download the system logo", description = "Returns the stored logo image bytes.")
    @APIResponse(responseCode = "200", description = "Logo image.", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = SchemaType.STRING, format = "binary")))
    @APIResponse(responseCode = "404", description = "System settings or logo were not configured yet.")
    public Response downloadLogo() {
        SystemSettings settings = requiredSettings();
        if (settings.logo == null || settings.logo.length == 0) {
            throw new NotFoundException("System logo was not uploaded yet");
        }
        return Response.ok(settings.logo, detectImageMediaType(settings.logo))
                .header("Content-Disposition", "attachment; filename=\"logo\"")
                .build();
    }

    @DELETE
    @Path("/logo")
    @Transactional
    @Operation(summary = "Remove the system logo", description = "Clears the stored logo image bytes.")
    @APIResponse(responseCode = "204", description = "Logo removed.")
    @APIResponse(responseCode = "404", description = "System settings were not configured yet.")
    public Response deleteLogo() {
        requiredSettings().logo = null;
        return Response.noContent().build();
    }

    // The content type is derived from the file signature so no extra column is needed.
    private static String detectImageMediaType(byte[] content) {
        if (content.length >= 8 && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N'
                && content[3] == 'G') {
            return "image/png";
        }
        if (content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (content.length >= 6 && content[0] == 'G' && content[1] == 'I' && content[2] == 'F') {
            return "image/gif";
        }
        if (content.length >= 4 && content[0] == '<'
                && (content[1] == '?' || (content[1] == 's' && content[2] == 'v' && content[3] == 'g'))) {
            return "image/svg+xml";
        }
        return MediaType.APPLICATION_OCTET_STREAM;
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
        settings.language = request.language() == null ? Language.DEFAULT : validateLanguage(request.language());
        settings.cursorApiKey = request.cursorApiKey();
        settings.cursorModel = request.cursorModel();
        settings.llmApiKey = request.llmApiKey();
        settings.llmModel = request.llmModel();
        settings.status = request.status() == null ? SettingsStatus.ACTIVE : request.status();
        settings.defaultExtractionMethod = request.defaultExtractionMethod() == null ? ExtractionMethod.ML
                : request.defaultExtractionMethod();
    }

    private static void merge(SystemSettingsRequest request, SystemSettings settings) {
        if (request.language() != null) {
            settings.language = validateLanguage(request.language());
        }
        if (request.cursorApiKey() != null) {
            settings.cursorApiKey = request.cursorApiKey();
        }
        if (request.cursorModel() != null) {
            settings.cursorModel = request.cursorModel();
        }
        if (request.llmApiKey() != null) {
            settings.llmApiKey = request.llmApiKey();
        }
        if (request.llmModel() != null) {
            settings.llmModel = request.llmModel();
        }
        if (request.status() != null) {
            settings.status = request.status();
        }
        if (request.defaultExtractionMethod() != null) {
            settings.defaultExtractionMethod = request.defaultExtractionMethod();
        }
    }

    private static String validateLanguage(String language) {
        try {
            return Language.validate(language);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }
}