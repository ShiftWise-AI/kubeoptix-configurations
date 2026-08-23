package com.shiftwise.ai.kubeoptix.documents;

import java.net.URI;
import java.util.List;
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

    @GET
    public List<VersionResponse> list() {
        return Version.<Version>listAll().stream().map(VersionResponse::from).toList();
    }

    @GET
    @Path("/{id}")
    public VersionResponse get(UUID id) {
        return VersionResponse.from(requiredVersion(id));
    }

    @POST
    @Transactional
    public Response create(VersionRequest request) {
        Version version = new Version();
        apply(request, version);
        version.persist();
        return Response.created(URI.create("/versions/" + version.id)).entity(VersionResponse.from(version)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public VersionResponse update(UUID id, VersionRequest request) {
        Version version = requiredVersion(id);
        apply(request, version);
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

    private static void apply(VersionRequest request, Version version) {
        if (request == null || request.versionNumber() == null || request.versionNumber().isBlank()) {
            throw new BadRequestException("versionNumber is required");
        }
        version.versionNumber = request.versionNumber();
        version.markdownContent = request.markdownContent();
    }
}