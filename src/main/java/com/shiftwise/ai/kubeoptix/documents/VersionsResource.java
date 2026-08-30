package com.shiftwise.ai.kubeoptix.documents;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/versions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class VersionsResource {

    private static final BigDecimal VERSION_STEP = new BigDecimal("0.1");

    @GET
    @Transactional
    public List<VersionResponse> list() {
        return Version.<Version>listAll().stream().map(VersionResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public VersionResponse get(UUID id) {
        return VersionResponse.from(requiredVersion(id));
    }

    @POST
    @Transactional
    public Response create(VersionRequest request) {
        validate(request);
        Document document = requiredDocument(request.documentName());
        Version latest = latestVersion(document);

        // No changes on any screen field since the last version: reuse it instead of duplicating.
        if (latest != null && sameContent(latest, request)) {
            return Response.ok(VersionResponse.from(latest)).build();
        }

        Version version = new Version();
        version.description = request.description();
        version.markdownContent = request.markdownContent();
        version.document = document;
        version.versionNumber = nextVersionNumber(latest);
        version.persist();
        return Response.created(URI.create("/versions/" + version.id)).entity(VersionResponse.from(version)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public VersionResponse update(UUID id, VersionRequest request) {
        validate(request);
        Version version = requiredVersion(id);
        version.description = request.description();
        version.markdownContent = request.markdownContent();
        version.document = requiredDocument(request.documentName());
        return VersionResponse.from(version);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(UUID id) {
        requiredVersion(id).delete();
        return Response.noContent().build();
    }

    private static Version requiredVersion(UUID id) {
        Version version = Version.findById(id);
        if (version == null) {
            throw new NotFoundException("Version not found: " + id);
        }
        return version;
    }

    private static void validate(VersionRequest request) {
        if (request == null || request.description() == null || request.description().isBlank()
                || request.documentName() == null || request.documentName().isBlank()) {
            throw new BadRequestException("description and documentName are required");
        }
    }

    private static Document requiredDocument(String documentName) {
        Document document = Document.findById(documentName);
        if (document == null) {
            throw new NotFoundException("Document not found: " + documentName);
        }
        return document;
    }

    private static Version latestVersion(Document document) {
        return Version.find("document.documentName = ?1 order by createdAt desc", document.documentName).firstResult();
    }

    private static boolean sameContent(Version latest, VersionRequest request) {
        return Objects.equals(latest.description, request.description())
                && Objects.equals(latest.markdownContent, request.markdownContent());
    }

    private static String nextVersionNumber(Version latest) {
        if (latest == null) {
            return "0.1";
        }
        try {
            BigDecimal current = new BigDecimal(latest.versionNumber);
            return current.add(VERSION_STEP).setScale(1, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            // Non-numeric version numbers (legacy data) get a numeric suffix appended instead.
            return latest.versionNumber + ".1";
        }
    }
}